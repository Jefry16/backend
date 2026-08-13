package com.vointika.storefront.presentation.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.vointika.shared.media.MediaUrlResolver;
import com.vointika.shared.port.MediaAssetBatchQuery.MediaAsset;
import com.vointika.shared.port.StorefrontExperienceQuery.ExperienceCardView;
import com.vointika.shared.port.StorefrontMetafieldQuery.MetafieldView;
import com.vointika.shared.port.StorefrontMetafieldQuery.MetaobjectView;
import com.vointika.storefront.application.dto.output.MenuData;
import com.vointika.storefront.application.dto.output.MenuData.MenuLinkData;
import com.vointika.shared.port.StorefrontTourOperatorQuery.AddressView;
import com.vointika.shared.port.StorefrontTourOperatorQuery.BrandView;
import com.vointika.shared.port.StorefrontTourOperatorQuery.ColorView;
import com.vointika.shared.port.StorefrontTourOperatorQuery.PolicyView;
import com.vointika.shared.port.StorefrontTourOperatorQuery.TourOperatorView;
import com.vointika.shared.port.StorefrontPageQuery.PageView;
import com.vointika.storefront.application.dto.output.StorefrontGlobals;
import com.vointika.storefront.application.policy.StorefrontRoutes;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * What every storefront address answers with, and — until a route has an object
 * of its own — the entire home page.
 *
 * <p><b>Names follow Shopify's, and renaming one is a breaking change</b> the day
 * an operator authors a theme. That is why the shape is decided here, against
 * JSON, while it costs a handful of records instead of a pile of templates.
 * Four deliberate departures, all recorded rather than accidental:
 *
 * <ul>
 *   <li><b>{@code tourOperator}, not Shopify's {@code shop}.</b> The object is
 *       the same one theirs is, and the name is the only thing that changed:
 *       nothing here sells from a shelf, the context that owns the row is
 *       {@code touroperator}, the admin serves it at
 *       {@code /api/tour-operators}, and the metafield owner type is already
 *       {@code TOUR_OPERATOR}. Shopify's name was the last place the platform's
 *       vocabulary outranked our own.
 *   <li><b>camelCase, not Liquid's snake_case.</b> A theme author arriving from
 *       Shopify types {@code short_description}; every other surface this project
 *       serves is camelCase, and one convention across the codebase beat one
 *       convention across an ecosystem we do not belong to.
 *   <li><b>{@code pageTitle}/{@code pageDescription} are top-level, and there is
 *       no {@code page} object.</b> In Shopify {@code page} is a CMS page, and it
 *       means the same thing here — so the current page's metadata uses their
 *       {@code page_title}/{@code page_description} globals and leaves the name
 *       free for {@code /pages/&#123;handle&#125;}.
 * </ul>
 *
 * <p><b>{@code ogImageUrl} has no Shopify counterpart</b>, and that is the
 * fourth departure: Shopify writes its own Open Graph tags through
 * {@code content_for_header}, which we have no equivalent of, so a theme needs
 * the value itself. {@code canonicalUrl} is theirs and {@code pageType} is
 * theirs flattened out of {@code request} — both here for the same reason, that
 * nothing writes the head for us.
 *
 * <p><b>{@code canonicalUrl} is always self-referencing</b>, and that is a
 * consequence of {@code LocaleRule} rather than a choice made here: the primary
 * locale lives at the bare path and {@code /{primary}} is a 404, so one page in
 * one language has exactly one address. It is worth serving anyway — a visit
 * carrying {@code ?utm_source=…} is the same page, and a self-referencing
 * canonical is what says so. Built from the <b>resolved</b> locale and handle,
 * never echoed back off the request, so it states the address we consider
 * authoritative rather than the one the client happened to type.
 */
public record StorefrontGlobalsResponse(TourOperator tourOperator,
                                        String pageTitle,
                                        String pageDescription,
                                        String ogImageUrl,
                                        String canonicalUrl,
                                        String pageType,
                                        Routes routes,
                                        List<ExperienceCard> featuredExperiences,
                                        Map<String, Menu> linklists,
                                        Localization localization,
                                        @JsonInclude(JsonInclude.Include.NON_NULL) Page page) {

    /**
     * Shopify's {@code request.page_type} values, for the addresses that serve
     * globals. A theme branches on this for JSON-LD's {@code @type} and for
     * analytics; it is the one part of their {@code request} object that is not
     * either theme-editor state or something already served
     * ({@code origin} is {@code tourOperator.url}, {@code locale} is
     * {@code localization.language}).
     *
     * <p>Each address names its own, at the {@link #from} overload that builds
     * it, rather than being inferred from which objects happen to be present. A
     * route whose object is absent is not automatically the index.
     */
    public static final String PAGE_TYPE_INDEX = "index";
    public static final String PAGE_TYPE_PAGE = "page";

    /**
     * @param description the meta description, which is what Shopify's
     *                    {@code tourOperator.description} is too — the same value
     *                    {@code pageDescription} carries on a page that has no
     *                    override of its own
     * @param passwordMessage public by design: it is shown to a visitor at the
     *                        gate. The password beside it in the database is not
     *                        here and must never be.
     */
    public record TourOperator(UUID id,
                               String name,
                               String handle,
                               String url,
                               String description,
                               Address address,
                               String phone,
                               String email,
                               String passwordMessage,
                               Currency currency,
                               Timezone timezone,
                               Brand brand,
                               List<Policy> policies,
                               Map<String, Map<String, Metafield>> metafields,
                               Policy cancellationPolicy,
                               Policy privacyPolicy,
                               Policy termsOfService,
                               Policy legalNotice) {}

    /**
     * Shopify's {@code address}, minus what an operator does not have.
     *
     * <p>Their {@code first_name}, {@code last_name}, {@code company},
     * {@code id} and {@code url} are customer-address fields; {@code province_code}
     * needs ISO 3166-2 reference data we do not carry. {@code street} is theirs
     * and derived — the two lines as one — so it costs nothing to give.
     * {@code summary} is skipped: its composition is Shopify-specific, and how an
     * address reads on a page is a theme's business.
     *
     * <p><b>Null when the operator has no address</b>, not an object of nulls —
     * the rule {@code Image} follows, so a template guards on the object.
     *
     * <p>{@code country.name} is <b>English only</b>, which is where we depart:
     * Shopify localizes it. The code rides alongside so a client can localize
     * with {@code Intl.DisplayNames} if it wants.
     */
    public record Address(String address1,
                          String address2,
                          String street,
                          String city,
                          String province,
                          String zip,
                          Country country) {}

    public record Country(String code, String name) {}

    public record Currency(String code, String symbol) {}

    public record Timezone(String name, String city) {}

    public record Brand(String slogan,
                        String shortDescription,
                        Colors colors,
                        Image logo,
                        Image squareLogo,
                        Image favicon,
                        Image coverImage,
                        List<SocialLink> socialLinks) {}

    public record Colors(List<Color> primary, List<Color> secondary) {}

    public record Color(String background, String foreground) {}

    public record SocialLink(String platform, String url) {}

    /**
     * Everything a theme renders as an {@code <img>}.
     *
     * <p>{@code aspectRatio} is <b>derived</b> and never stored — a third column
     * can disagree with the two it comes from. An absent media reference is a
     * <b>null Image</b> rather than an Image with a null url, so a template
     * guards on the object.
     */
    public record Image(String url, String alt, Integer width, Integer height, Double aspectRatio) {}

    /**
     * @param type the enum name; {@code url} carries the slug, because
     *             {@code LEGAL_NOTICE} addresses {@code /policies/legal-notice}
     */
    public record Policy(UUID id, String type, String title, String url) {}

    /**
     * Shopify's metafield object, minus what we do not have.
     *
     * <p>Addressed the way theirs is — {@code tourOperator.metafields.namespace.key} —
     * because that is what a theme author types. <b>{@code type} is our
     * vocabulary, not theirs</b>: {@code single_line_text}, never
     * {@code single_line_text_field}, decided when the context shipped.
     *
     * <p>There is no {@code list?}, because list types are deliberately not in
     * our catalogue and a field that is always false is invention. And unlike
     * Liquid, where a metafield renders its own value, a JSON consumer reads
     * {@code .value} — that is what JSON costs, not a choice.
     *
     * <p><b>{@code value} is a string for every scalar type and a
     * {@link Metaobject} for {@code metaobject_reference}</b>, which is Liquid's
     * own model: their docs say a reference type's {@code value} "directly
     * returns the referenced object", and there is no separate {@code reference}
     * property to copy. So the pair stays {@code {type, value}} and a theme
     * reads {@code type} to know which it got. A reference whose target is
     * unpublished or gone is <b>not here at all</b> — the port prunes it, the
     * way a menu prunes a dead link.
     */
    public record Metafield(String type, Object value) {}

    /**
     * A resolved metaobject entry.
     *
     * <p><b>Fields are nested, where Shopify's are top-level.</b> Theirs are
     * accessors on the drop ({@code boat.capacity.value}), which forces a
     * {@code system} object — their docs say it exists "to avoid collisions
     * between system property names and user-defined metaobject fields". Nesting
     * under {@code fields} makes a collision impossible, so {@code system} buys
     * nothing here and is not copied.
     *
     * <p>There is no {@code url}: Shopify gives an entry its own page at
     * {@code /metaobjects/{type}/{handle}} and we have no such route. A URL for
     * an address that does not exist is invention (PATTERNS §5).
     *
     * @param fields keyed by field key, in the definition's field order, and
     *               carrying only the fields that have a value
     */
    public record Metaobject(UUID id,
                             String type,
                             String handle,
                             String name,
                             Map<String, MetaobjectField> fields) {}

    public record MetaobjectField(String type, String value) {}

    public record Routes(String root, String experiences) {}

    /**
     * A CMS page — <b>the object this route is associated with</b>, absent from
     * every other address rather than present and null. That is Liquid's model:
     * a template gets the globals plus its own object, and the others simply are
     * not defined.
     *
     * <p>The name is Shopify's and means what theirs means, which is why the
     * current page's metadata is {@code pageTitle}/{@code pageDescription} at the
     * top level and not here.
     *
     * @param body raw HTML the operator authored, unescaped by design — our
     *             rendering of their content, the same trust boundary the policy
     *             page sits on
     * @param url  this page's own address, prefix included, so a canonical tag
     *             does not have to be assembled from parts
     */
    public record Page(UUID id, String handle, String title, String body, String url) {}

    /**
     * One menu, addressed by handle the way Shopify's is —
     * {@code linklists["main-menu"].links}.
     */
    public record Menu(String title, List<Link> links) {}

    /**
     * Shopify's {@code link}, minus what we do not have and what depends on the
     * request.
     *
     * <p><b>No {@code handle}</b>: theirs is a slug of the link's title and we
     * store no such column, so it would be invented. <b>No {@code active},
     * {@code current}, {@code child_active} or {@code child_current}</b>: those
     * are computed from the page being rendered, which would make them the first
     * fields in the globals whose value depends on which address you asked for.
     * They can be added when a theme needs them.
     *
     * <p><b>Every link here points somewhere that exists.</b> Items whose target
     * was unpublished or deleted are gone, along with anything nested under them.
     *
     * @param type   our vocabulary — {@code HOME}, {@code EXPERIENCE_LIST},
     *               {@code EXPERIENCE}, {@code PAGE}, {@code EXTERNAL_URL} — not
     *               Shopify's {@code page_link}/{@code product_link} forms, the
     *               same call as the metafield type codes
     * @param levels how deep the tree runs below this link. Derived, like
     *               {@code aspectRatio}: a theme uses it to decide whether a link
     *               needs a dropdown at all.
     */
    public record Link(String title, String type, String url, int levels, List<Link> links) {}

    /**
     * One featured experience, top-level rather than under {@code tourOperator} — it is
     * catalogue, which Shopify also keeps beside its {@code shop} object rather than
     * inside it.
     *
     * <p><b>{@code startingPrice} is a string.</b> It is a decimal amount, and a
     * JSON number is a double on the far side of most parsers, which loses cents
     * on values a customer is asked to pay. The currency it is in is
     * {@code tourOperator.currency} — there is one per operator, so repeating it per card
     * would be a field that cannot vary.
     *
     * <p><b>{@code url} points at a route that does not exist yet.</b> The
     * experience detail page is unbuilt; the link is what gives a localized
     * handle a reader, and it is recorded as a known gap rather than left to
     * surprise someone.
     */
    public record ExperienceCard(UUID id,
                                 String handle,
                                 String name,
                                 String description,
                                 String startingPrice,
                                 String url,
                                 Image thumbnail) {}

    /**
     * @param language  the one being served
     * @param languages every locale the operator publishes, primary first
     */
    public record Localization(Language language, List<Language> languages) {}

    /**
     * @param name        the language written in the operator's primary locale —
     *                    "francés" for a Spanish operator. Shopify's {@code name}.
     * @param endonymName the language written in itself — "français"
     * @param primary     whether this is the operator's primary locale, <b>not</b>
     *                    whether it is the one being served: that is
     *                    {@code localization.language}
     */
    public record Language(String code, String name, String endonymName, boolean primary, String url) {}

    /**
     * Every media id the response will need, so the caller can resolve them in
     * <b>one</b> batch rather than one lookup per image. Kept beside
     * {@link #from} so the two cannot drift apart.
     */
    public static Set<UUID> mediaIds(StorefrontGlobals globals) {
        Set<UUID> ids = new LinkedHashSet<>();
        add(ids, globals.ogImageMediaId());
        BrandView brand = globals.tourOperator().brand();
        add(ids, brand.logoMediaId());
        add(ids, brand.squareLogoMediaId());
        add(ids, brand.faviconMediaId());
        add(ids, brand.coverImageMediaId());
        // The cards' thumbnails ride the same batch. Resolving them separately
        // would be one lookup per card on every page.
        for (ExperienceCardView card : globals.featuredExperiences()) {
            add(ids, card.thumbnailMediaId());
        }
        return ids;
    }

    /**
     * @param origin scheme and host of the request, which is what {@code tourOperator.url}
     *               is. Taken from the request rather than from configuration so
     *               it stays correct behind a proxy, the same reason the tenant is
     *               read from {@code getServerName()}.
     */
    public static StorefrontGlobalsResponse from(StorefrontGlobals globals,
                                                 String origin,
                                                 Map<UUID, MediaAsset> assets,
                                                 MediaUrlResolver urls) {
        return from(globals, null, PAGE_TYPE_INDEX, origin, assets, urls);
    }

    /** The same globals, plus the object the route is associated with. */
    public static StorefrontGlobalsResponse from(StorefrontGlobals globals,
                                                 PageView page,
                                                 String origin,
                                                 Map<UUID, MediaAsset> assets,
                                                 MediaUrlResolver urls) {
        return from(globals, page, PAGE_TYPE_PAGE, origin, assets, urls);
    }

    private static StorefrontGlobalsResponse from(StorefrontGlobals globals,
                                                  PageView page,
                                                  String pageType,
                                                  String origin,
                                                  Map<UUID, MediaAsset> assets,
                                                  MediaUrlResolver urls) {
        TourOperatorView tourOperator = globals.tourOperator();
        String prefix = localePrefix(globals);
        List<Policy> policies = tourOperator.policies().stream()
                .map(p -> policy(p, prefix))
                .toList();
        Image ogImage = image(globals.ogImageMediaId(), assets, urls);
        String root = prefix.isEmpty() ? StorefrontRoutes.HOME : prefix;
        String pagePath = page == null ? null : prefix + StorefrontRoutes.PAGES + "/" + page.handle();

        return new StorefrontGlobalsResponse(
                new TourOperator(
                        tourOperator.id(),
                        tourOperator.name(),
                        tourOperator.handle(),
                        origin,
                        tourOperator.seoDescription(),
                        address(tourOperator.address()),
                        tourOperator.phone(),
                        tourOperator.email(),
                        tourOperator.passwordMessage(),
                        new Currency(tourOperator.currencyCode(), tourOperator.currencySymbol()),
                        new Timezone(tourOperator.timezoneName(), tourOperator.timezoneCity()),
                        brand(tourOperator.brand(), assets, urls),
                        policies,
                        metafields(globals.metafields()),
                        named(policies, "CANCELLATION"),
                        named(policies, "PRIVACY"),
                        named(policies, "TERMS"),
                        named(policies, "LEGAL_NOTICE")),
                globals.pageTitle(),
                globals.pageDescription(),
                ogImage == null ? null : ogImage.url(),
                origin + (pagePath == null ? root : pagePath),
                pageType,
                new Routes(root, prefix + StorefrontRoutes.EXPERIENCES),
                globals.featuredExperiences().stream()
                        .map(card -> card(card, prefix, assets, urls))
                        .toList(),
                linklists(globals.menus(), prefix),
                localization(globals),
                page == null ? null : new Page(page.id(), page.handle(), page.title(), page.body(),
                        pagePath));
    }

    /** Keyed by handle, insertion-ordered on the query's handle ordering. */
    private static Map<String, Menu> linklists(List<MenuData> menus, String prefix) {
        Map<String, Menu> byHandle = new LinkedHashMap<>();
        for (MenuData menu : menus) {
            byHandle.put(menu.handle(), new Menu(menu.title(), links(menu.links(), prefix)));
        }
        return byHandle;
    }

    private static List<Link> links(List<MenuLinkData> links, String prefix) {
        return links.stream().map(link -> {
            List<Link> children = links(link.links(), prefix);
            return new Link(link.title(), link.linkType(), linkUrl(link, prefix),
                    depth(children), children);
        }).toList();
    }

    /**
     * <b>Every internal link carries the locale prefix</b>, or a visitor reading
     * in Spanish clicks the header and leaves the language. An
     * {@code EXTERNAL_URL} is absolute and passes through untouched.
     */
    private static String linkUrl(MenuLinkData link, String prefix) {
        return switch (link.linkType()) {
            case "HOME" -> prefix.isEmpty() ? StorefrontRoutes.HOME : prefix;
            case "EXPERIENCE_LIST" -> prefix + StorefrontRoutes.EXPERIENCES;
            case "EXPERIENCE" -> prefix + StorefrontRoutes.EXPERIENCES + "/" + link.targetHandle();
            case "PAGE" -> prefix + StorefrontRoutes.PAGES + "/" + link.targetHandle();
            default -> link.externalUrl();
        };
    }

    private static int depth(List<Link> children) {
        return children.stream().mapToInt(child -> 1 + child.levels()).max().orElse(0);
    }

    private static ExperienceCard card(ExperienceCardView card, String prefix,
                                       Map<UUID, MediaAsset> assets, MediaUrlResolver urls) {
        return new ExperienceCard(
                card.id(),
                card.handle(),
                card.name(),
                card.description(),
                card.startingPrice() == null ? null : card.startingPrice().toPlainString(),
                prefix + StorefrontRoutes.EXPERIENCES + "/" + card.handle(),
                image(card.thumbnailMediaId(), assets, urls));
    }

    private static Address address(AddressView address) {
        if (address == null) {
            return null;
        }
        return new Address(address.address1(), address.address2(), address.street(),
                address.city(), address.province(), address.zip(),
                address.countryCode() == null
                        ? null : new Country(address.countryCode(), address.countryName()));
    }

    private static Brand brand(BrandView brand, Map<UUID, MediaAsset> assets, MediaUrlResolver urls) {
        return new Brand(
                brand.slogan(),
                brand.shortDescription(),
                new Colors(colors(brand.primaryColors()), colors(brand.secondaryColors())),
                image(brand.logoMediaId(), assets, urls),
                image(brand.squareLogoMediaId(), assets, urls),
                image(brand.faviconMediaId(), assets, urls),
                image(brand.coverImageMediaId(), assets, urls),
                brand.socialLinks().stream()
                        .map(l -> new SocialLink(l.platform(), l.url()))
                        .toList());
    }

    private static List<Color> colors(List<ColorView> views) {
        return views.stream().map(c -> new Color(c.background(), c.foreground())).toList();
    }

    private static Image image(UUID mediaId, Map<UUID, MediaAsset> assets, MediaUrlResolver urls) {
        if (mediaId == null) {
            return null;
        }
        MediaAsset asset = assets.get(mediaId);
        if (asset == null) {
            return null;
        }
        return new Image(urls.toUrl(asset.storageKey()), asset.alt(), asset.width(), asset.height(),
                aspectRatio(asset.width(), asset.height()));
    }

    private static Double aspectRatio(Integer width, Integer height) {
        if (width == null || height == null || height == 0) {
            return null;
        }
        return (double) width / height;
    }

    private static Policy policy(PolicyView view, String prefix) {
        String slug = view.type().toLowerCase(Locale.ROOT).replace('_', '-');
        return new Policy(view.id(), view.type(), view.title(),
                prefix + StorefrontRoutes.POLICIES + "/" + slug);
    }

    /**
     * Namespace then key, both insertion-ordered: the query already sorts, and a
     * payload that reshuffles between requests is a diff nobody can read.
     */
    private static Map<String, Map<String, Metafield>> metafields(List<MetafieldView> values) {
        Map<String, Map<String, Metafield>> byNamespace = new LinkedHashMap<>();
        for (MetafieldView value : values) {
            byNamespace.computeIfAbsent(value.namespace(), n -> new LinkedHashMap<>())
                    .put(value.key(), new Metafield(value.type(), metafieldValue(value)));
        }
        return byNamespace;
    }

    /** Liquid's rule: a reference resolves to its object, everything else is text. */
    private static Object metafieldValue(MetafieldView value) {
        MetaobjectView target = value.metaobject();
        if (target == null) {
            return value.value();
        }
        Map<String, MetaobjectField> fields = new LinkedHashMap<>();
        target.fields().forEach(f -> fields.put(f.key(), new MetaobjectField(f.type(), f.value())));
        return new Metaobject(target.id(), target.type(), target.handle(), target.name(), fields);
    }

    private static Policy named(List<Policy> policies, String type) {
        return policies.stream().filter(p -> p.type().equals(type)).findFirst().orElse(null);
    }

    /**
     * The switcher needs "this page in that language". On the home page that is
     * each locale's root, so it is built here; the first route whose path varies
     * per page is the one that has to pass its own.
     */
    private static Localization localization(StorefrontGlobals globals) {
        StorefrontGlobals.LocalizationData data = globals.localization();
        List<Language> languages = new ArrayList<>();
        for (String code : data.supported()) {
            languages.add(language(code, data.primary(), data.primary().equals(code) ? "/" : "/" + code));
        }
        Language current = languages.stream()
                .filter(l -> l.code().equals(data.current()))
                .findFirst()
                .orElse(null);
        return new Localization(current, List.copyOf(languages));
    }

    /**
     * Both names come from the JDK's CLDR data rather than a curated column.
     * Shopify's {@code name} is the language rendered <em>in the operator's primary
     * locale</em>, which is one value per <em>pair</em> of languages — six
     * languages is thirty-six of them, so it is not a table anyone maintains.
     * {@code getDisplayName} rather than {@code getDisplayLanguage} because it
     * keeps the region: {@code pt-BR} and {@code pt-PT} are both "português"
     * without it, which makes a two-Portuguese switcher unusable. An unknown code
     * comes back as itself, so nothing here can produce a null.
     */
    private static Language language(String code, String primaryLocale, String url) {
        Locale locale = Locale.forLanguageTag(code);
        return new Language(
                code,
                locale.getDisplayName(Locale.forLanguageTag(primaryLocale)),
                locale.getDisplayName(locale),
                code.equals(primaryLocale),
                url);
    }

    private static String localePrefix(StorefrontGlobals globals) {
        StorefrontGlobals.LocalizationData data = globals.localization();
        return data.current().equals(data.primary()) ? "" : "/" + data.current();
    }

    private static void add(Set<UUID> ids, UUID id) {
        if (id != null) {
            ids.add(id);
        }
    }
}
