package org.verapdf.rest.resources;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileFilter;

import com.codahale.metrics.annotation.Timed;
import io.swagger.annotations.ApiOperation;
import org.apache.commons.io.filefilter.WildcardFileFilter;

import java.nio.file.Files;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.Paths;
import java.nio.file.FileVisitResult;
import java.nio.file.attribute.BasicFileAttributes;
import java.security.DigestInputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Collections;
import java.util.EnumSet;
import java.util.List;
import java.util.ArrayList;

import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;
import javax.xml.transform.TransformerException;

import org.apache.commons.codec.binary.Hex;
import org.apache.commons.io.IOUtils;
import org.glassfish.jersey.media.multipart.FormDataContentDisposition;
import org.glassfish.jersey.media.multipart.FormDataParam;
import org.verapdf.core.ModelParsingException;
import org.verapdf.core.VeraPDFException;
import org.verapdf.core.EncryptedPdfException;
import org.verapdf.features.FeatureFactory;
import org.verapdf.metadata.fixer.FixerFactory;
import org.verapdf.pdfa.Foundries;
import org.verapdf.pdfa.PDFAParser;
import org.verapdf.pdfa.PDFAValidator;
import org.verapdf.pdfa.VeraGreenfieldFoundryProvider;
import org.verapdf.pdfa.flavours.PDFAFlavour;
import org.verapdf.pdfa.results.ValidationResult;
import org.verapdf.features.FeatureExtractorConfig;
import org.verapdf.metadata.fixer.MetadataFixerConfig;
import org.verapdf.pdfa.validation.validators.ValidatorConfig;
import org.verapdf.pdfa.validation.validators.ValidatorFactory;
import org.verapdf.processor.BatchProcessor;
import org.verapdf.processor.FormatOption;
import org.verapdf.processor.ProcessorConfig;
import org.verapdf.processor.ProcessorFactory;
import org.verapdf.processor.TaskType;
import org.verapdf.processor.plugins.PluginsCollectionConfig;
import org.verapdf.processor.reports.BatchSummary;
import org.verapdf.report.HTMLReport;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author <a href="mailto:carl@openpreservation.org">Carl Wilson</a>
 * @author <a href="mailto:rstorey@loc.gov">Rosie Storey</a>
 */

@Timed
public class ValidateResource {
	// java.security.digest name for the SHA-1 algorithm
	private static final String SHA1_NAME = "SHA-1"; //$NON-NLS-1$
	private static final String AUTODETECT_PROFILE = "auto"; //$NON-NLS-1$
	private static final String WIKI_URL_BASE = "https://github.com/veraPDF/veraPDF-validation-profiles/wiki/"; //$NON-NLS-1$
	private static final Boolean LOG_SUCCESS_CHECKS = false;
	private static final int MAX_FAILED_CHECKS_PER_RULE = 100;
	private static final Boolean VERBOSE_OUTPUT = false;
	private static final String TEMP_FILE_PREFIX = "veraPDF-cache-"; //$NON-NLS-1$
	private static final String TEMP_FILE_SUFFIX = ".pdf";
	private static final Logger LOGGER = LoggerFactory.getLogger(ValidateResource.class);
	static {
		VeraGreenfieldFoundryProvider.initialise();
	}

	/**
	 * @param profileId
	 *            the String id of the Validation profile (auto, 1b, 1a, 2b, 2a, 2u,
	 *            3b, 3a, or 3u)
	 * @param sha1Hex
	 *            the hex String representation of the file's SHA-1 hash
	 * @param uploadedInputStream
	 *            a {@link java.io.InputStream} to the PDF to be validated
	 * @param contentDispositionHeader
	 * 			  the {@link org.glassfish.jersey.media.multipart.FormDataContentDisposition}
	 * @return the {@link org.verapdf.pdfa.results.ValidationResult} obtained
	 *         when validating the uploaded stream against the selected profile.
	 * @throws VeraPDFException
	 * 			  throws {@link org.verapdf.core.VeraPDFException} for any exception from core library
	 */
	@POST
	@ApiOperation( 	value = "Upload and validate a PDF file",
					notes = "Profile Ids: auto, 1b, 1a, 2b, 2a, 2u, 3b, 3a, 3u",
					response = ValidationResult.class)
	@Path("/{profileId}")
	@Consumes(MediaType.MULTIPART_FORM_DATA)
	@Produces({ MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML })
	public static ValidationResult validatePost(@PathParam("profileId") String profileId,
			                                    @FormDataParam("sha1Hex") String sha1Hex,
                                                @FormDataParam("file") InputStream uploadedInputStream,
			                                    @FormDataParam("file") final FormDataContentDisposition contentDispositionHeader)
            throws VeraPDFException {

		LOGGER.info("Received a POST validate request for profileId: {} with sha1Hex: {}", profileId, sha1Hex);
	    return validate(profileId, sha1Hex, uploadedInputStream);

	}

	/**
	 * @param directoryPath
	 * 			  the String of a path on the local disk with PDFs to validate
	 * @return an {@link java.io.InputStream} of HTML with validation report results
	 * @throws VeraPDFException
	 * 			  throws {@link org.verapdf.core.VeraPDFException} for any exception from core library
	 */
	@GET
	@ApiOperation(	value = "Validate a single file or directory of files on the local disk",
					notes = "Recursively finds PDF files in the specified path and performs auto-detect validation")
	@Path("/processFiles")
	@Produces({ MediaType.TEXT_HTML })
	public static InputStream processFilesGet(@QueryParam("directoryPath") String directoryPath)
			throws VeraPDFException {

		LOGGER.info("Received a GET processFiles request with directoryPath: {}", directoryPath);
		return validateFilePath(directoryPath);
	}

	/**
	 * @param profileId
	 *            the String id of the Validation profile (auto, 1b, 1a, 2b, 2a, 2u,
	 *            3b, 3a, or 3u)
	 * @param headers
	 *         the {@link javax.ws.rs.core.HttpHeaders} context of this request
	 * @param inFile
	 *            byte array of the PDF to be validated
	 * @return the {@link org.verapdf.pdfa.results.ValidationResult} obtained
	 *         when validating the uploaded stream against the selected profile.
	 * @throws VeraPDFException
	 * 			  throws {@link org.verapdf.core.VeraPDFException} for any exception from core library
	 */
	@PUT
	@ApiOperation(	value = "Upload and validate a single file according to the specified profile Id",
					notes = "Profile Ids: auto, 1b, 1a, 2b, 2a, 2u, 3b, 3a, 3u",
					response = ValidationResult.class)
	@Path("/{profileId}")
	@Consumes( MediaType.WILDCARD)
	@Produces({ MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML })
	public static ValidationResult validatePut(@PathParam("profileId") String profileId,
											   @Context HttpHeaders headers,
											   byte[] inFile)
			throws VeraPDFException {

		LOGGER.info("Received a PUT validate request for profileId: {}", profileId);
		InputStream fileInputStream = new ByteArrayInputStream(inFile);

		return validate(profileId, null, fileInputStream);

	}

	/**
	 * @param profileId
	 *            the String id of the Validation profile (auto, 1b, 1a, 2b, 2a, 2u,
	 *            3b, 3a, or 3u)
	 * @param sha1Hex
	 *            the hex String representation of the file's SHA-1 hash
	 * @param uploadedInputStream
	 *            a {@link java.io.InputStream} to the PDF to be validated
	 * @param contentDispositionHeader
	 * 			  the {@link org.glassfish.jersey.media.multipart.FormDataContentDisposition}
	 * @return an {@link java.io.InputStream} of HTML with validation report results
	 * @throws VeraPDFException
	 * 			  throws {@link org.verapdf.core.VeraPDFException} for any exception from core library
	 */
	@POST
	@ApiOperation( value = "Upload and validate a single PDF file",
					notes = "ProfileIds: auto, 1b, 1a, 2b, 2a, 2u, 3b, 3a, 3u")
	@Path("/{profileId}")
	@Consumes(MediaType.MULTIPART_FORM_DATA)
	@Produces({ MediaType.TEXT_HTML })
	public static InputStream validateHtml(@PathParam("profileId") String profileId,
                                           @FormDataParam("sha1Hex") String sha1Hex,
                                           @FormDataParam("file") InputStream uploadedInputStream,
                                           @FormDataParam("file") final FormDataContentDisposition contentDispositionHeader)
                                                throws VeraPDFException {

		File file;
		List<File> files;
		FileInputStream flavourDetectStream;
		PDFAFlavour flavour;

		LOGGER.info("Received a POST validate HTML request with profileId:{} sha1Hex: {}", profileId, sha1Hex);

		LOGGER.trace("Saving uploaded file to temp file on local disk");
		file = saveUploadedFileToTemp(uploadedInputStream);

		if(!profileId.equals(AUTODETECT_PROFILE)) {
			LOGGER.trace("ProfileId is not auto-detect");
			flavour = PDFAFlavour.byFlavourId(profileId);
		} else {
			LOGGER.trace("Auto-detecting profile");
			try {
				flavourDetectStream = new FileInputStream(file);
				flavour = detectFlavour(flavourDetectStream);
				LOGGER.trace("Profile type {} was auto-detected", flavour.toString());

			} catch(FileNotFoundException exception) {
				LOGGER.error("Problem detecting profile from uploaded file", exception);
				throw new VeraPDFException("Problem detecting profile from uploaded file", exception);
			}
		}

		try {
			uploadedInputStream.close();
		} catch(IOException exception) {
			LOGGER.error("An exception occurred reading the uploaded file", exception);
			throw new VeraPDFException("An exception occurred reading the uploaded file", exception);
		}


		files = Collections.singletonList(file);

		LOGGER.trace("Validating and preparing HTML report for {} files", files.size());
		return processFilesCreateHtmlReport(null, files, flavour);
	}

	/*
		Save the file which was uploaded through http(s) to a temp location on the local disk.
	 */
	private static File saveUploadedFileToTemp(InputStream uploadedInputStream) throws VeraPDFException {
		File file;

		try {
			LOGGER.trace("Creating a temp file with prefix {} and suffix {}", TEMP_FILE_PREFIX, TEMP_FILE_SUFFIX);
			file = File.createTempFile(TEMP_FILE_PREFIX, TEMP_FILE_SUFFIX);
		} catch (IOException exception) {
			LOGGER.error("IOException creating a temp file", exception);
			throw new VeraPDFException("IOException creating a temp file", exception); //$NON-NLS-1$
		}

		try (OutputStream fos = new FileOutputStream(file)) {
			LOGGER.trace("Copying the uploadedInputStream to the newly created temp file with name {}",
					file.getAbsoluteFile());
			IOUtils.copy(uploadedInputStream, fos);
		} catch (IOException exception) {
			LOGGER.error("IOException writing to temp file", exception);
			throw new VeraPDFException("IOException writing to temp file", exception); //$NON-NLS-1$
		}

		return file;
	}

	private static ByteArrayInputStream processFilesCreateHtmlReport(String directoryPath, List<File> files,
																	 PDFAFlavour flavour)
		throws VeraPDFException {

		BatchProcessor processor;
		BatchSummary summary;
        ProcessorConfig processorConfig = configureProcessor(flavour);

		byte[] htmlBytes;
		InputStream xmlBis;
		ByteArrayOutputStream htmlBos;

		LOGGER.trace("Processing {} files to create an HTML report", files.size());
		try (ByteArrayOutputStream xmlBos = new ByteArrayOutputStream()) {
			processor = ProcessorFactory.fileBatchProcessor(processorConfig);
			summary = processor.process(files,
					ProcessorFactory.getHandler(FormatOption.MRR, VERBOSE_OUTPUT, xmlBos,
							MAX_FAILED_CHECKS_PER_RULE, LOG_SUCCESS_CHECKS));

			xmlBis = new ByteArrayInputStream(xmlBos.toByteArray());
			htmlBos = new ByteArrayOutputStream();
			HTMLReport.writeHTMLReport(xmlBis, htmlBos, summary, WIKI_URL_BASE, VERBOSE_OUTPUT);
			htmlBytes = htmlBos.toByteArray();
			if(directoryPath!=null) {
				createVerifyEventInCTS(directoryPath, htmlBytes);
			} else {
				LOGGER.trace("Directory path is null, so because this is an uploaded file, not adding CTS event.");
			}
		} catch (IOException | TransformerException exception) {
			LOGGER.error("An exception occurred while processing files for an HTML report", exception);
			throw new VeraPDFException("An exception occurred while validating", exception); //$NON-NLS-1$
		}

		return new ByteArrayInputStream(htmlBytes);
	}

	private static void createVerifyEventInCTS(String directoryPath, byte[] htmlBytes) {
		LOGGER.trace("(NOT IMPLEMENTED) Creating Verify event in CTS for bag instance with path {}", directoryPath);
		// TODO: Look up whether there is any inventoried, non-empty bag instance with this directory path
		// (Currently not supported by CTS web API)

		// TODO: If one or more bag instances are found, create a verify event for each one
		// https://cts.loc.gov/transfer/inventory/event/VerifyEvent takes a POST request with parameters:
		// bagInstanceKey or bagInstanceVersionKey (one is required)
		// eventMessage - optional
		// eventStart - optional
		// eventEnd - optional
		// destFilepath - required
		// spec - required
		// conformance - optional
		// isSuccess - optional
		// eventSystemId - optional

		// TODO: Determine whether all files passed validation or not to set the isSuccess param

		// TODO: Upload the full report as an event attachment (not currently supported in CTS web API)

	}

	/*
	This method is used for PUT and POST non-HTML-based validation of a single uploaded file.
	Sha1 for the uploaded file may be provided or may be null.
	The profile validation flavour may be specified or may be auto-detect.
	 */
    private static ValidationResult validate(String profileId, String sha1Hex,
                                        InputStream uploadedInputStream)
            throws VeraPDFException {

    	/* Perform our own calculation of the sha-1 of the uploaded file */
        MessageDigest sha1 = getDigest();
        DigestInputStream digestInputStream = new DigestInputStream(uploadedInputStream, sha1);

        ValidationResult result;
        PDFAFlavour flavour;
        PDFAParser parser;
        PDFAValidator validator;

		try {

			if(!profileId.equals(AUTODETECT_PROFILE)) {
				LOGGER.trace("Using specified profile flavour for validation {}", profileId);
				/* Use the specified profile flavour for validation */
				flavour = PDFAFlavour.byFlavourId(profileId);
			} else {
				LOGGER.trace("Auto-detecting profile for uploaded input stream");
				/* Don't specify a profile flavour for validation - use veraPDF to autodetect the profile */
				flavour = detectFlavour(digestInputStream);
			}

            parser = Foundries.defaultInstance().createParser(digestInputStream, flavour);
			validator = ValidatorFactory.createValidator(flavour, LOG_SUCCESS_CHECKS);
			result = validator.validate(parser);
		} catch (ModelParsingException mpException) {
			/*
			If we have the same sha-1 then it's a PDF parse error, so
			treat as non PDF.
			*/
            LOGGER.error("Caught a model parsing exception during validation", mpException);
			if(sha1Hex!=null) {
				if (sha1Hex.equalsIgnoreCase(Hex.encodeHexString(sha1.digest()))) {
					LOGGER.error("File does not appear to be a PDF");
					throw new NotSupportedException(Response.status(Status.UNSUPPORTED_MEDIA_TYPE)
						.type(MediaType.TEXT_PLAIN).entity("File does not appear " +
								 "to be a PDF.").build(), mpException); //$NON-NLS-1$ //$NON-NLS-2$
				}
			}
			throw mpException;
		}

        return result;
    }

	private static MessageDigest getDigest() {
		try {
			return MessageDigest.getInstance(SHA1_NAME);
		} catch (NoSuchAlgorithmException nsaException) {
			LOGGER.error("No digest algorithm implementation for {}", SHA1_NAME, nsaException);
			throw new IllegalStateException(
					"No digest algorithm implementation for " +
                            SHA1_NAME, nsaException); //$NON-NLS-1$
		}
	}

	private static EnumSet<TaskType> getTasks() {
		EnumSet<TaskType> tasks = EnumSet.noneOf(TaskType.class);
        tasks.add(TaskType.VALIDATE);
        // TODO: implement options to run these tasks
//		tasks.add(TaskType.EXTRACT_FEATURES);
//		tasks.add(TaskType.FIX_METADATA);
		return tasks;
	}

	private static ProcessorConfig configureProcessor(PDFAFlavour flavour) {

        ValidatorConfig validatorConfig;

        if(flavour==null) {
            LOGGER.trace("Creating default validation configuration");
            validatorConfig = ValidatorFactory.defaultConfig();
        } else {
            LOGGER.trace("Creating validation configuration with flavour {}, LOG_SUCCESS_CHECKS {}, " +
                    "MAX_FAILED_CHECKS_PER_RULE {}", flavour.toString(), LOG_SUCCESS_CHECKS, MAX_FAILED_CHECKS_PER_RULE);
            validatorConfig = ValidatorFactory.createConfig(flavour, LOG_SUCCESS_CHECKS, MAX_FAILED_CHECKS_PER_RULE);
        }
        FeatureExtractorConfig featureConfig = FeatureFactory.defaultConfig();
        PluginsCollectionConfig pluginsConfig = PluginsCollectionConfig.defaultConfig();
        MetadataFixerConfig fixerConfig = FixerFactory.defaultConfig();

        LOGGER.trace("Creating processor config with default values");
        return ProcessorFactory.fromValues(validatorConfig, featureConfig,
                pluginsConfig, fixerConfig, getTasks());

    }


	private static InputStream validateFilePath(String filePath) throws VeraPDFException {
		File directoryPath = new File(filePath);
        final FileFilter fileFilter = new WildcardFileFilter("*.pdf*");
        List<File> files = new ArrayList<>();

        class RecursivePDFFinder extends SimpleFileVisitor<java.nio.file.Path> {

            List<File> pdfFiles = new ArrayList<>();

            @Override
            public FileVisitResult preVisitDirectory(java.nio.file.Path dir,
                    BasicFileAttributes attributes) {
                LOGGER.trace("Directory: " + dir.toString());
                File thisDirectory = new File(dir.toString());
                Collections.addAll(pdfFiles, thisDirectory.listFiles(fileFilter));
                return FileVisitResult.CONTINUE;
            }

            public List<File> getPdfFiles() {
                return this.pdfFiles;
            }
        }

        RecursivePDFFinder pdfFinder = new RecursivePDFFinder();

		// The specified filePath may either be a path to a single PDF or a path to a directory.
		// If it is a directory, recursively list all PDF files (files having extension containing pdf)
		// and run the processor on all of them.
		if(directoryPath.isDirectory()) {
			LOGGER.trace("Validating directory of files at {}", filePath);

            try {
                // TODO: implement options for following or ignoring symbolic links
                Files.walkFileTree(Paths.get(filePath), pdfFinder);
            } catch (IOException exception) {
                LOGGER.error("An exception occurred while finding PDF files", exception);
                throw new VeraPDFException("An exception occurred while finding PDF files", exception);
            }

            files = pdfFinder.getPdfFiles();
            LOGGER.trace("Recursively found {} PDF files", files.size());

		} else {
			LOGGER.trace("Validating individual file at {}", filePath);
			files.add(new File(filePath));
		}

		if(!files.isEmpty()) {
            return processFilesCreateHtmlReport(filePath, files, null);
        } else {
		    LOGGER.debug("No files to process, so not preparing HTML report");
		    return null;
        }
	}

    private static PDFAFlavour detectFlavour(InputStream detectStream)
            throws ModelParsingException, EncryptedPdfException {

        PDFAParser parser = Foundries.defaultInstance().createParser(detectStream);
        return parser.getFlavour();

    }

}
