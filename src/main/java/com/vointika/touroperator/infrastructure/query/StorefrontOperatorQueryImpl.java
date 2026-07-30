package com.vointika.touroperator.infrastructure.query;

import com.vointika.reference.domain.entity.Currency;
import com.vointika.reference.domain.entity.Timezone;
import com.vointika.reference.domain.repository.CurrencyRepository;
import com.vointika.reference.domain.repository.TimezoneRepository;
import com.vointika.shared.media.MediaUrlBatchResolver;
import com.vointika.shared.port.StorefrontOperatorQuery;
import com.vointika.shared.port.StorefrontOperatorView;
import com.vointika.shared.valueobject.LocaleCode;
import com.vointika.touroperator.domain.entity.TourOperator;
import com.vointika.touroperator.domain.repository.TourOperatorRepository;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

/**
 * touroperator's adapter for the shared {@link StorefrontOperatorQuery} seam:
 * resolves a tenant from its storefront slug and hands back everything the
 * public site needs about the operator, fully resolved.
 *
 * <p>Three reference/media lookups ride along (logo id → URL, currency → code,
 * timezone → IANA name) so the caller — the {@code rendering} context, which
 * imports no bounded context at all — never has to join anything itself.
 */
@Component
public class StorefrontOperatorQueryImpl implements StorefrontOperatorQuery {

    private final TourOperatorRepository tourOperatorRepository;
    private final CurrencyRepository currencyRepository;
    private final TimezoneRepository timezoneRepository;
    private final MediaUrlBatchResolver mediaUrlBatchResolver;

    public StorefrontOperatorQueryImpl(TourOperatorRepository tourOperatorRepository,
                                       CurrencyRepository currencyRepository,
                                       TimezoneRepository timezoneRepository,
                                       MediaUrlBatchResolver mediaUrlBatchResolver) {
        this.tourOperatorRepository = tourOperatorRepository;
        this.currencyRepository = currencyRepository;
        this.timezoneRepository = timezoneRepository;
        this.mediaUrlBatchResolver = mediaUrlBatchResolver;
    }

    @Override
    public Optional<StorefrontOperatorView> findBySlug(String slug) {
        return tourOperatorRepository.findBySlug(slug).map(this::toView);
    }

    @Override
    public boolean verifyStorefrontPassword(String slug, String candidate) {
        if (candidate == null) {
            return false;
        }
        return tourOperatorRepository.findBySlug(slug)
                .filter(TourOperator::isPasswordEnabled)
                .map(TourOperator::getStorefrontPassword)
                .filter(stored -> stored != null && matches(stored, candidate))
                .isPresent();
    }

    private StorefrontOperatorView toView(TourOperator operator) {
        String primaryLocale = operator.getPrimaryLocale().value();
        return new StorefrontOperatorView(
                operator.getId(),
                operator.getName().value(),
                operator.getSlug().value(),
                mediaUrlBatchResolver.resolveOne(operator.getId(), operator.getLogoMediaId()),
                primaryLocale,
                orderedLocales(operator, primaryLocale),
                currencyRepository.findById(operator.getCurrencyId())
                        .map(Currency::getCode)
                        .orElse(null),
                timezoneRepository.findById(operator.getTimezoneId())
                        .map(Timezone::getName)
                        .orElse(null),
                operator.isPasswordEnabled(),
                operator.getPasswordMessage());
    }

    /** Primary first, then the remaining published locales alphabetically. */
    private List<String> orderedLocales(TourOperator operator, String primaryLocale) {
        return operator.getSupportedLocales().stream()
                .map(LocaleCode::value)
                .sorted(Comparator.comparing((String code) -> !code.equals(primaryLocale))
                        .thenComparing(Comparator.naturalOrder()))
                .toList();
    }

    /**
     * Constant-time comparison of fixed-width SHA-256 digests, matching
     * {@code InternalApiSecretFilter}. The storefront password is a shared gate
     * rather than a credential, but it is still a secret compared against
     * attacker-supplied input: hashing first means the comparison leaks neither
     * the stored password's length nor its prefix through timing.
     */
    private boolean matches(String stored, String candidate) {
        return MessageDigest.isEqual(sha256(stored), sha256(candidate));
    }

    private byte[] sha256(String value) {
        try {
            return MessageDigest.getInstance("SHA-256")
                    .digest(value.getBytes(StandardCharsets.UTF_8));
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 is required by the Java platform", e);
        }
    }
}
