package com.vointika.storefront.presentation.response;

import com.vointika.storefront.application.dto.output.PasswordPageOutput;

/**
 * What a locked storefront answers with. Deliberately tiny: everything here is
 * public by construction, and everything the gate hides is not read at all.
 *
 * <p>It is <b>not</b> the globals. A visitor in front of the gate has no business
 * receiving the brand, the contact details, the policies or the published
 * locales — the gate exists to withhold exactly that, and a partial {@code tourOperator}
 * object here would misrepresent the shape a theme gets on a real page anyway.
 *
 * @param error true when a submitted password was refused. Every refusal looks
 *              the same — wrong password, unknown tenant, gate with no password
 *              set — so the form tells a visitor nothing beyond "not this".
 */
public record PasswordPageResponse(String operatorName, String passwordMessage, boolean error) {

    public static PasswordPageResponse from(PasswordPageOutput output, boolean error) {
        return new PasswordPageResponse(output.operatorName(), output.passwordMessage(), error);
    }
}
