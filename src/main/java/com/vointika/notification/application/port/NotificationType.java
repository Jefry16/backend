package com.vointika.notification.application.port;

import java.util.Locale;

/**
 * The emails this platform sends. One constant per template pair on the classpath.
 *
 * <p><b>It exists because the type used to be a bare string, and a typo in one was
 * invisible.</b> A consumer passing {@code "VERIFICATON_EMAIL"} compiled, shipped, and
 * at runtime {@code SendNotificationUseCase} found no template, logged
 * {@code "No template found"} and returned — so that email silently stopped and the
 * build stayed green. Verified before this enum landed: a one-character edit passed the
 * full suite. As a type, the same mistake does not compile.
 *
 * <p><b>The declared universe is now {@link #values()} rather than a second list.</b>
 * {@code ClasspathTemplateCatalog} kept a {@code KNOWN_TEMPLATES} map whose every entry
 * pointed at the same locale list, so the map's only job was naming the types — which is
 * what an enum is. Adding an email is a constant here plus its file pair; the catalog's
 * fail-fast loader refuses to start if the files are missing, and
 * {@code TemplateLocalesTrackUiLanguagesTest} refuses if a UI language has none.
 *
 * <p>The constant name <em>is</em> the filename stem: {@code VERIFICATION_EMAIL} →
 * {@code verification-email_{locale}.html} and {@code .subject.txt}. {@link #fileBase()}
 * is that derivation, in one place, so a rename cannot drift from the files it names.
 */
public enum NotificationType {

    VERIFICATION_EMAIL,
    PASSWORD_RESET_EMAIL,
    PASSWORD_CHANGED_EMAIL,
    ACCOUNT_ALREADY_REGISTERED_EMAIL,
    TOUR_OPERATOR_WELCOME_EMAIL,
    TEAM_INVITATION_EMAIL;

    /**
     * {@code VERIFICATION_EMAIL} → {@code verification-email}, the templates' filename stem.
     *
     * <p><b>{@link Locale#ROOT}, never the JVM default</b> (PATTERNS §11). Every constant
     * here contains {@code EMAIL}, so every one has an {@code I} to lose: under a Turkish
     * default this folds to {@code verıfıcatıon-emaıl} — dotless — and no template file
     * matches. All six, measured rather than reasoned. The eager loader turns that into a
     * refuses-to-start rather than a silent non-send, but it is invisible on every machine
     * anyone develops or runs CI on, which is what makes it a deployment landmine.
     */
    public String fileBase() {
        return name().toLowerCase(Locale.ROOT).replace('_', '-');
    }
}
