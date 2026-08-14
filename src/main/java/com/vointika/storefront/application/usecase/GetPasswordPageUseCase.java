package com.vointika.storefront.application.usecase;

import com.vointika.shared.port.StorefrontTourOperatorQuery;
import com.vointika.storefront.application.dto.output.PasswordPageOutput;

import java.util.Optional;

/**
 * The copy the gate shows: the operator's name and the operator's message for
 * visitors, in the <b>primary locale</b>.
 *
 * <p>Primary-locale only is a consequence of the gate running first, and it is a
 * real limitation rather than an oversight. Localizing this page by path would
 * mean reading the path locale before the gate, and answering differently for a
 * supported and an unsupported locale is exactly the leak the ordering prevents.
 *
 * <p>It reads the gate and nothing else. The operator's name is not translated (V8
 * left it off the overlay on purpose — a brand name is not content), so there is
 * no second read to make.
 */
public class GetPasswordPageUseCase {

    private final StorefrontTourOperatorQuery storefrontTourOperatorQuery;

    public GetPasswordPageUseCase(StorefrontTourOperatorQuery storefrontTourOperatorQuery) {
        this.storefrontTourOperatorQuery = storefrontTourOperatorQuery;
    }

    public Optional<PasswordPageOutput> execute(String handle) {
        return storefrontTourOperatorQuery.findGate(handle)
                .map(gate -> new PasswordPageOutput(gate.operatorName(), gate.passwordMessage()));
    }
}
