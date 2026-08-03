package com.vointika.storefront.application.usecase;

import com.vointika.shared.port.StorefrontShopQuery;
import com.vointika.shared.port.StorefrontShopQuery.StorefrontGateView;
import com.vointika.shared.port.StorefrontShopQuery.StorefrontShopView;
import com.vointika.storefront.application.dto.output.PasswordPageOutput;

import java.util.Optional;

/**
 * The copy the password page shows: the shop's name and the operator's message
 * for visitors, in the <b>primary locale</b>.
 *
 * <p>Primary-locale only is a consequence of the gate running first, and it is a
 * real limitation rather than an oversight. Localizing this page by path would
 * mean reading the path locale before the gate, and answering differently for a
 * supported and an unsupported locale is exactly the leak the ordering prevents.
 */
public class GetPasswordPageUseCase {

    private final StorefrontShopQuery storefrontShopQuery;

    public GetPasswordPageUseCase(StorefrontShopQuery storefrontShopQuery) {
        this.storefrontShopQuery = storefrontShopQuery;
    }

    public Optional<PasswordPageOutput> execute(String handle) {
        return storefrontShopQuery.findGate(handle).flatMap(this::toOutput);
    }

    private Optional<PasswordPageOutput> toOutput(StorefrontGateView gate) {
        return storefrontShopQuery.findContent(gate.tourOperatorId(), gate.primaryLocale())
                .map(StorefrontShopView::name)
                .map(name -> new PasswordPageOutput(name, gate.passwordMessage()));
    }
}
