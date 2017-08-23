package org.verapdf.rest.resources;

import java.util.HashSet;
import java.util.Set;

import javax.inject.Singleton;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

import io.swagger.annotations.ApiOperation;
import org.verapdf.pdfa.flavours.PDFAFlavour;
import org.verapdf.pdfa.validation.profiles.ProfileDetails;
import org.verapdf.pdfa.validation.profiles.ProfileDirectory;
import org.verapdf.pdfa.validation.profiles.Profiles;
import org.verapdf.pdfa.validation.profiles.Rule;
import org.verapdf.pdfa.validation.profiles.RuleId;
import org.verapdf.pdfa.validation.profiles.ValidationProfile;

/**
 * @author <a href="mailto:carl@openpreservation.org">Carl Wilson</a>
 */

@Singleton
public class ProfileResource {
	private static final ProfileDirectory DIRECTORY = Profiles.getVeraProfileDirectory();
	private static final Set<ProfileDetails> DETAILS = new HashSet<>();
	static {
		for (ValidationProfile profile : DIRECTORY.getValidationProfiles()) {
			DETAILS.add(profile.getDetails());
		}
	}

	/**
	 * @return the set of validation profile details
	 */
	@GET
	@ApiOperation(	value = "Get a set of details about available PDF/A validation profiles",
					responseContainer = "Set",
					response = ProfileDetails.class)
	@Produces({ MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML })
	public static Set<ProfileDetails> getProfileDetails() {
		return DETAILS;
	}

	/**
	 * @return the Set of Validation Profile IDs
	 */
	@GET
	@ApiOperation(	value = "Get a set of 2-letter ID codes for available PDF/A validation profiles",
					notes = "1b, 1a, 2b, 2a, 2u, 3b, 3a, 3u",
					response = String.class,
					responseContainer = "Set")
	@Path("/ids")
	@Produces({ MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML })
	public static Set<String> getProfileIds() {
		return DIRECTORY.getValidationProfileIds();
	}

	/**
	 * @return the Set of PDF/A Flavours
	 */
	@GET
	@ApiOperation(	value = "Get the set of PDF/A flavours",
					responseContainer = "Set",
					response = PDFAFlavour.class)
	@Path("/flavours")
	@Produces({ MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML })
	public static Set<PDFAFlavour> getFlavours() {
		return DIRECTORY.getPDFAFlavours();
	}

	/**
	 * @param profileId
	 *            the String id of the Validation profile (1b, 1a, 2b, 2a, 2u,
	 *            3b, 3a, or 3u)
	 * @return a validation profile selected by id
	 */
	@GET
	@ApiOperation(	value = "Get the validation profile for the specified 2-letter profileId code",
					notes = "1b, 1a, 2b, 2a, 2u, 3b, 3a, 3u",
					response = ValidationProfile.class)
	@Path("/{profileId}")
	@Produces({ MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML })
	public static ValidationProfile getProfile(@PathParam("profileId") String profileId) {
		return DIRECTORY.getValidationProfileById(profileId);
	}

	/**
	 * @param profileId
	 *            the String id of the Validation profile (1b, 1a, 2b, 2a, 2u,
	 *            3b, 3a, or 3u)
	 * @return the {@link java.util.Set} of
	 *         {@link org.verapdf.pdfa.validation.profiles.RuleId}s for the selected
	 *         Validation Profile
	 */
	@GET
	@ApiOperation(	value = "Get the set of rule IDs for the specified validation profile",
					notes = "1b, 1a, 2b, 2a, 2u, 3b, 3a, 3u",
					response = RuleId.class,
					responseContainer = "Set")
	@Path("/{profileId}/ruleids")
	@Produces({ MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML })
	public static Set<RuleId> getProfileRules(@PathParam("profileId") String profileId) {
		Set<RuleId> ids = new HashSet<>();
		for (Rule rule : DIRECTORY.getValidationProfileById(profileId).getRules()) {
			ids.add(rule.getRuleId());
		}
		return ids;
	}

	/**
	 * @param profileId
	 *            the String id of the Validation profile (1b, 1a, 2b, 2a, 2u,
	 *            3b, 3a, or 3u)
	 * @param clause
	 *            a {@link java.lang.String} identifying the profile clause to
	 *            return the Rules for
	 * @return the {@link java.util.Set} of
	 *         {@link org.verapdf.pdfa.validation.profiles.Rule}s for the selected
	 *         profile and clause
	 */
	@GET
	@ApiOperation(	value = "Get the set of rules for the selected profile and clause",
					notes = "1b, 1a, 2b, 2a, 2u, 3b, 3a, 3u",
					response = Rule.class,
					responseContainer = "Set")
	@Path("/{profileId}/{clause}")
	@Produces({ MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML })
	public static Set<Rule> getRulesForClause(@PathParam("profileId") String profileId,
			@PathParam("clause") String clause) {
		Set<Rule> rules = new HashSet<>();
		for (Rule rule : DIRECTORY.getValidationProfileById(profileId).getRules()) {
			if (rule.getRuleId().getClause().equalsIgnoreCase(clause)) {
				rules.add(rule);
			}
		}
		return rules;
	}
}
