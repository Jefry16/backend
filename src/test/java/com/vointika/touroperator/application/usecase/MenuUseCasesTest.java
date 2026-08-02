package com.vointika.touroperator.application.usecase;

import com.vointika.shared.exception.InvalidFieldException;
import com.vointika.shared.exception.ResourceAlreadyExistsException;
import com.vointika.shared.exception.ResourceNotFoundException;
import com.vointika.shared.list.CursorPage;
import com.vointika.shared.list.ListQuery;
import com.vointika.shared.port.AuditTrailPort;
import com.vointika.shared.port.ExperienceOwnershipQuery;
import com.vointika.shared.port.NewAuditEntry;
import com.vointika.shared.port.PageOwnershipQuery;
import com.vointika.shared.port.TourOperatorMembershipCheck;
import com.vointika.shared.port.TransactionRunner;
import com.vointika.shared.service.IdGenerator;
import com.vointika.shared.valueobject.LocaleCode;
import com.vointika.shared.valueobject.Handle;
import com.vointika.touroperator.application.dto.input.CreateMenuInput;
import com.vointika.touroperator.application.dto.input.RenameMenuInput;
import com.vointika.touroperator.application.dto.input.ReplaceMenuItemsInput;
import com.vointika.touroperator.application.dto.input.ReplaceMenuItemsInput.MenuItemInput;
import com.vointika.touroperator.application.dto.output.MenuDetail;
import com.vointika.touroperator.domain.entity.Menu;
import com.vointika.touroperator.domain.entity.MenuItem;
import com.vointika.touroperator.domain.entity.MenuItemTranslation;
import com.vointika.touroperator.domain.entity.TourOperator;
import com.vointika.touroperator.domain.enums.MenuItemLinkType;
import com.vointika.touroperator.domain.repository.MenuItemRepository;
import com.vointika.touroperator.domain.repository.MenuRepository;
import com.vointika.touroperator.domain.repository.TourOperatorRepository;
import com.vointika.touroperator.domain.valueobject.TourOperatorAddress;
import com.vointika.touroperator.domain.valueobject.TourOperatorName;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class MenuUseCasesTest {

    private static final UUID OP = UUID.fromString("019f7f33-1833-7dc1-b008-47e6c68b3ea2");
    private static final UUID USER = UUID.fromString("550e8400-e29b-41d4-a716-446655440000");
    private static final UUID MENU = UUID.fromString("aaaaaaaa-0000-4000-8000-000000000001");
    private static final UUID EXPERIENCE = UUID.fromString("bbbbbbbb-0000-4000-8000-000000000001");
    private static final UUID PAGE = UUID.fromString("cccccccc-0000-4000-8000-000000000001");

    private MenuRepository menuRepository;
    private MenuItemRepository menuItemRepository;
    private TourOperatorRepository tourOperatorRepository;
    private ExperienceOwnershipQuery experienceOwnershipQuery;
    private PageOwnershipQuery pageOwnershipQuery;
    private TourOperatorMembershipCheck membershipCheck;
    private TransactionRunner transactionRunner;
    private IdGenerator idGenerator;
    private AuditTrailPort auditTrailPort;

    @BeforeEach
    void setUp() {
        menuRepository = mock(MenuRepository.class);
        menuItemRepository = mock(MenuItemRepository.class);
        tourOperatorRepository = mock(TourOperatorRepository.class);
        experienceOwnershipQuery = mock(ExperienceOwnershipQuery.class);
        pageOwnershipQuery = mock(PageOwnershipQuery.class);
        membershipCheck = mock(TourOperatorMembershipCheck.class);
        transactionRunner = mock(TransactionRunner.class);
        idGenerator = mock(IdGenerator.class);
        auditTrailPort = mock(AuditTrailPort.class);
        doAnswer(i -> {
            ((Runnable) i.getArgument(0)).run();
            return null;
        }).when(transactionRunner).run(any());
        when(idGenerator.newId()).thenAnswer(i -> UUID.randomUUID());
        when(menuRepository.save(any())).thenAnswer(i -> i.getArgument(0));
        when(menuRepository.findByIdAndTourOperatorId(MENU, OP))
                .thenReturn(Optional.of(menu()));
        when(tourOperatorRepository.findById(OP)).thenReturn(Optional.of(operator()));
        when(experienceOwnershipQuery.existsForTourOperator(EXPERIENCE, OP)).thenReturn(true);
        when(pageOwnershipQuery.existsForTourOperator(PAGE, OP)).thenReturn(true);
    }

    private Menu menu() {
        return new Menu(MENU, OP, new Handle("main-menu"), "Main menu", USER,
                Instant.parse("2026-07-01T10:00:00Z"), Instant.parse("2026-07-01T10:00:00Z"));
    }

    private TourOperator operator() {
        return new TourOperator(OP, new TourOperatorName("Acme Tours"), new Handle("acme"),
                UUID.randomUUID(), UUID.randomUUID(), new TourOperatorAddress("Calle Mayor 1"),
                USER, Instant.now(), Instant.now(), null,
                LocaleCode.of("en"), Set.of(LocaleCode.of("en"), LocaleCode.of("es")));
    }

    private ReplaceMenuItemsUseCase replaceUseCase() {
        return new ReplaceMenuItemsUseCase(menuRepository, menuItemRepository,
                tourOperatorRepository, experienceOwnershipQuery, pageOwnershipQuery,
                membershipCheck, idGenerator, transactionRunner, auditTrailPort);
    }

    private static MenuItemInput item(String title, String linkType, UUID resourceId, String url,
                                      Map<String, String> translations, List<MenuItemInput> children) {
        return new MenuItemInput(title, linkType, resourceId, url, translations, children);
    }

    // ------------------------------------------------------------------ create

    @Test
    void createSavesMenuAndAudits() {
        CreateMenuUseCase useCase = new CreateMenuUseCase(menuRepository, membershipCheck,
                idGenerator, transactionRunner, auditTrailPort);

        UUID id = useCase.execute(new CreateMenuInput(USER, OP, "legal", "Legal"));

        assertThat(id).isNotNull();
        verify(membershipCheck).ensureAdmin(USER, OP);
        ArgumentCaptor<Menu> saved = ArgumentCaptor.forClass(Menu.class);
        verify(menuRepository).save(saved.capture());
        assertThat(saved.getValue().getHandle().value()).isEqualTo("legal");
        assertThat(saved.getValue().getTitle()).isEqualTo("Legal");
        ArgumentCaptor<NewAuditEntry> audit = ArgumentCaptor.forClass(NewAuditEntry.class);
        verify(auditTrailPort).append(audit.capture());
        assertThat(audit.getValue().action()).isEqualTo("menu.created");
    }

    @Test
    void createRejectsDuplicateHandle() {
        when(menuRepository.existsByTourOperatorIdAndHandle(OP, "main-menu")).thenReturn(true);
        CreateMenuUseCase useCase = new CreateMenuUseCase(menuRepository, membershipCheck,
                idGenerator, transactionRunner, auditTrailPort);

        assertThatThrownBy(() -> useCase.execute(new CreateMenuInput(USER, OP, "main-menu", "Again")))
                .isInstanceOf(ResourceAlreadyExistsException.class);
        verify(menuRepository, never()).save(any());
    }

    @Test
    void createRejectsBadHandle() {
        CreateMenuUseCase useCase = new CreateMenuUseCase(menuRepository, membershipCheck,
                idGenerator, transactionRunner, auditTrailPort);

        assertThatThrownBy(() -> useCase.execute(new CreateMenuInput(USER, OP, "Main Menu!", "Main")))
                .isInstanceOf(InvalidFieldException.class);
    }

    // ------------------------------------------------------------------ rename

    @Test
    void renameSavesAndAuditsWithChanges() {
        RenameMenuUseCase useCase = new RenameMenuUseCase(menuRepository, membershipCheck,
                transactionRunner, auditTrailPort);

        useCase.execute(new RenameMenuInput(USER, OP, MENU, "Primary navigation"));

        verify(membershipCheck).ensureAdmin(USER, OP);
        ArgumentCaptor<Menu> saved = ArgumentCaptor.forClass(Menu.class);
        verify(menuRepository).save(saved.capture());
        assertThat(saved.getValue().getTitle()).isEqualTo("Primary navigation");
        ArgumentCaptor<NewAuditEntry> audit = ArgumentCaptor.forClass(NewAuditEntry.class);
        verify(auditTrailPort).append(audit.capture());
        assertThat(audit.getValue().action()).isEqualTo("menu.renamed");
        assertThat(audit.getValue().changes()).isNotEmpty();
    }

    @Test
    void renameToSameTitleRecordsNothing() {
        RenameMenuUseCase useCase = new RenameMenuUseCase(menuRepository, membershipCheck,
                transactionRunner, auditTrailPort);

        useCase.execute(new RenameMenuInput(USER, OP, MENU, "Main menu"));

        verify(menuRepository).save(any());
        verify(auditTrailPort, never()).append(any());
    }

    @Test
    void renameUnknownMenuIs404() {
        when(menuRepository.findByIdAndTourOperatorId(MENU, OP)).thenReturn(Optional.empty());
        RenameMenuUseCase useCase = new RenameMenuUseCase(menuRepository, membershipCheck,
                transactionRunner, auditTrailPort);

        assertThatThrownBy(() -> useCase.execute(new RenameMenuInput(USER, OP, MENU, "X")))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    // ------------------------------------------------------------------ delete

    @Test
    void deleteRemovesMenuAndAudits() {
        DeleteMenuUseCase useCase = new DeleteMenuUseCase(menuRepository, membershipCheck,
                transactionRunner, auditTrailPort);

        useCase.execute(OP, MENU, USER);

        verify(membershipCheck).ensureAdmin(USER, OP);
        verify(menuRepository).delete(MENU);
        ArgumentCaptor<NewAuditEntry> audit = ArgumentCaptor.forClass(NewAuditEntry.class);
        verify(auditTrailPort).append(audit.capture());
        assertThat(audit.getValue().action()).isEqualTo("menu.deleted");
    }

    // ------------------------------------------------------------------ list / get

    @Test
    void listRequiresMembership() {
        ListMenusUseCase useCase = new ListMenusUseCase(menuRepository, membershipCheck);
        ListQuery query = mock(ListQuery.class);
        when(query.tenantId()).thenReturn(OP);
        when(menuRepository.list(query)).thenReturn(new CursorPage<>(List.of(menu()), null));

        CursorPage<Menu> page = useCase.execute(query, USER);

        verify(membershipCheck).ensureMember(USER, OP);
        assertThat(page.data()).hasSize(1);
    }

    @Test
    void getAssemblesTreeWithTranslations() {
        UUID top = UUID.randomUUID();
        UUID child = UUID.randomUUID();
        Instant now = Instant.now();
        when(menuItemRepository.findByMenuId(MENU)).thenReturn(List.of(
                new MenuItem(top, MENU, null, "Tours", MenuItemLinkType.EXPERIENCE_LIST,
                        null, null, 0, now, now),
                new MenuItem(child, MENU, top, "Sunset sail", MenuItemLinkType.EXPERIENCE,
                        EXPERIENCE, null, 0, now, now)));
        when(menuItemRepository.findTranslationsByMenuId(MENU)).thenReturn(List.of(
                new MenuItemTranslation(top, LocaleCode.of("es"), "Paseos")));
        GetMenuUseCase useCase = new GetMenuUseCase(menuRepository, menuItemRepository,
                membershipCheck);

        MenuDetail detail = useCase.execute(OP, MENU, USER);

        verify(membershipCheck).ensureMember(USER, OP);
        assertThat(detail.items()).hasSize(1);
        assertThat(detail.items().getFirst().title()).isEqualTo("Tours");
        assertThat(detail.items().getFirst().titleTranslations()).containsEntry("es", "Paseos");
        assertThat(detail.items().getFirst().children()).hasSize(1);
        assertThat(detail.items().getFirst().children().getFirst().resourceId()).isEqualTo(EXPERIENCE);
    }

    // ------------------------------------------------------------------ replace items

    @Test
    void replaceFlattensTreeAssigningPositionsAndDepths() {
        replaceUseCase().execute(new ReplaceMenuItemsInput(USER, OP, MENU, List.of(
                item("Home", "HOME", null, null, Map.of("es", "Inicio"), List.of()),
                item("Explore", "EXPERIENCE_LIST", null, null, null, List.of(
                        item("Sunset sail", "EXPERIENCE", EXPERIENCE, null, null, List.of()),
                        item("About", "PAGE", PAGE, null, null, List.of()))))));

        verify(membershipCheck).ensureAdmin(USER, OP);
        verify(menuItemRepository).deleteByMenuId(MENU);
        @SuppressWarnings("unchecked")
        ArgumentCaptor<List<MenuItem>> items = ArgumentCaptor.forClass(List.class);
        verify(menuItemRepository).saveAll(items.capture());
        assertThat(items.getValue()).hasSize(4);
        MenuItem home = items.getValue().get(0);
        MenuItem explore = items.getValue().get(1);
        MenuItem sunset = items.getValue().get(2);
        assertThat(home.getParentId()).isNull();
        assertThat(home.getPosition()).isZero();
        assertThat(explore.getPosition()).isEqualTo(1);
        assertThat(sunset.getParentId()).isEqualTo(explore.getId());
        assertThat(sunset.getPosition()).isZero();
        @SuppressWarnings("unchecked")
        ArgumentCaptor<List<MenuItemTranslation>> translations = ArgumentCaptor.forClass(List.class);
        verify(menuItemRepository).saveAllTranslations(translations.capture());
        assertThat(translations.getValue()).hasSize(1);
        assertThat(translations.getValue().getFirst().menuItemId()).isEqualTo(home.getId());
        ArgumentCaptor<NewAuditEntry> audit = ArgumentCaptor.forClass(NewAuditEntry.class);
        verify(auditTrailPort).append(audit.capture());
        assertThat(audit.getValue().action()).isEqualTo("menu.items_replaced");
        assertThat(audit.getValue().details()).containsEntry("itemCount", 4);
    }

    @Test
    void replaceRejectsTooDeepTreeBeforeAnyWrite() {
        MenuItemInput level4 = item("L4", "HOME", null, null, null, List.of());
        MenuItemInput level3 = item("L3", "HOME", null, null, null, List.of(level4));
        MenuItemInput level2 = item("L2", "HOME", null, null, null, List.of(level3));
        MenuItemInput level1 = item("L1", "HOME", null, null, null, List.of(level2));

        assertThatThrownBy(() -> replaceUseCase().execute(
                new ReplaceMenuItemsInput(USER, OP, MENU, List.of(level1))))
                .isInstanceOf(InvalidFieldException.class)
                .hasMessageContaining("3 levels");
        verify(menuItemRepository, never()).deleteByMenuId(any());
        verify(menuItemRepository, never()).saveAll(any());
    }

    @Test
    void replaceRejectsLinkPayloadMismatch() {
        assertThatThrownBy(() -> replaceUseCase().execute(
                new ReplaceMenuItemsInput(USER, OP, MENU, List.of(
                        item("Broken", "EXPERIENCE", null, null, null, List.of())))))
                .isInstanceOf(InvalidFieldException.class)
                .hasMessageContaining("resourceId");
    }

    @Test
    void replaceRejectsUnknownLinkType() {
        assertThatThrownBy(() -> replaceUseCase().execute(
                new ReplaceMenuItemsInput(USER, OP, MENU, List.of(
                        item("Cart", "CART", null, null, null, List.of())))))
                .isInstanceOf(InvalidFieldException.class)
                .hasMessageContaining("Unknown menu item link type");
    }

    @Test
    void replaceRejectsForeignExperience() {
        when(experienceOwnershipQuery.existsForTourOperator(EXPERIENCE, OP)).thenReturn(false);

        assertThatThrownBy(() -> replaceUseCase().execute(
                new ReplaceMenuItemsInput(USER, OP, MENU, List.of(
                        item("Sneaky", "EXPERIENCE", EXPERIENCE, null, null, List.of())))))
                .isInstanceOf(InvalidFieldException.class)
                .hasMessageContaining("operator's experiences");
        verify(menuItemRepository, never()).deleteByMenuId(any());
    }

    @Test
    void replaceRejectsForeignPage() {
        when(pageOwnershipQuery.existsForTourOperator(PAGE, OP)).thenReturn(false);

        assertThatThrownBy(() -> replaceUseCase().execute(
                new ReplaceMenuItemsInput(USER, OP, MENU, List.of(
                        item("Sneaky", "PAGE", PAGE, null, null, List.of())))))
                .isInstanceOf(InvalidFieldException.class)
                .hasMessageContaining("operator's pages");
    }

    @Test
    void replaceRejectsUnsupportedTranslationLocale() {
        assertThatThrownBy(() -> replaceUseCase().execute(
                new ReplaceMenuItemsInput(USER, OP, MENU, List.of(
                        item("Home", "HOME", null, null, Map.of("fr", "Accueil"), List.of())))))
                .isInstanceOf(InvalidFieldException.class)
                .hasMessageContaining("supported locales");
        verify(menuItemRepository, never()).deleteByMenuId(any());
    }

    @Test
    void replaceWithEmptyTreeClearsItems() {
        replaceUseCase().execute(new ReplaceMenuItemsInput(USER, OP, MENU, List.of()));

        verify(menuItemRepository).deleteByMenuId(MENU);
        ArgumentCaptor<NewAuditEntry> audit = ArgumentCaptor.forClass(NewAuditEntry.class);
        verify(auditTrailPort).append(audit.capture());
        assertThat(audit.getValue().details()).containsEntry("itemCount", 0);
    }
}
