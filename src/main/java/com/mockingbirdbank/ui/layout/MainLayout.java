package com.mockingbirdbank.ui.layout;

import com.mockingbirdbank.model.AccountHolder;
import com.mockingbirdbank.service.AccountService;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.applayout.AppLayout;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.html.H1;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.orderedlayout.FlexLayout;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.spring.security.AuthenticationContext;
import com.vaadin.flow.theme.lumo.LumoUtility;
import jakarta.annotation.security.PermitAll;

/**
 * The dark top bar shared by every view: the bird wordmark on the left, the signed-in holder's name
 * and a sign-out button (wired to Spring Security's logout via Vaadin's {@link
 * AuthenticationContext}) on the right. Individual views render everything below it.
 *
 * <p>Vaadin's navigation access control requires a parent layout to grant access independently of
 * its views - without {@code @PermitAll} here, every view routed through this layout would 403 even
 * if the view itself is {@code @PermitAll}, because an unannotated layout defaults to
 * {@code @DenyAll}.
 */
@PermitAll
public class MainLayout extends AppLayout {

    private static final String THEME_STORAGE_KEY = "mbb-theme";
    private static final String DARK_THEME = "dark";

    // AuthenticationContext isn't Serializable by design; Vaadin view
    // fields that hold it must be transient.
    private final transient AuthenticationContext authenticationContext;

    public MainLayout(AccountService accountService, AuthenticationContext authenticationContext) {
        this.authenticationContext = authenticationContext;
        AccountHolder holder = accountService.currentHolder();

        BirdMark mark = new BirdMark();

        H1 wordmark = new H1("Mockingbird Bank");
        wordmark.addClassNames(LumoUtility.FontSize.XLARGE, LumoUtility.Margin.NONE);
        wordmark.getStyle().set("font-family", "Georgia, serif").set("color", "var(--mb-ivory)");

        HorizontalLayout brand = new HorizontalLayout(mark, wordmark);
        brand.setAlignItems(FlexLayout.Alignment.CENTER);
        brand.setSpacing(true);

        Span greeting = new Span("Welcome back, " + holder.getFullName());
        greeting.getStyle().set("color", "var(--mb-ivory-muted)").set("font-size", "0.9rem");

        Button themeToggle = new Button("Dark mode");
        themeToggle.getElement().setAttribute("theme", "tertiary");
        themeToggle.getStyle().set("color", "var(--mb-ivory)").set("border", "1px solid #3C4E6E");
        themeToggle.addClickListener(click -> toggleTheme());

        Button signOut = new Button("Sign out");
        signOut.getElement().setAttribute("theme", "tertiary");
        signOut.getStyle().set("color", "var(--mb-ivory)").set("border", "1px solid #3C4E6E");
        signOut.addClickListener(click -> authenticationContext.logout());

        applyStoredTheme();

        HorizontalLayout right = new HorizontalLayout(greeting, themeToggle, signOut);
        right.setAlignItems(FlexLayout.Alignment.CENTER);
        right.setSpacing(true);

        HorizontalLayout bar = new HorizontalLayout(brand, right);
        bar.setWidthFull();
        bar.setAlignItems(FlexLayout.Alignment.CENTER);
        bar.setJustifyContentMode(FlexLayout.JustifyContentMode.BETWEEN);
        bar.addClassNames(
                LumoUtility.Padding.Horizontal.LARGE, LumoUtility.Padding.Vertical.MEDIUM);
        bar.getStyle().set("background", "var(--mb-navy)");

        addToNavbar(bar);
    }

    // On a fresh full-page load the browser briefly renders light mode before this JS runs
    // (no document.documentElement attribute to read server-side pre-render) - an acceptable
    // flash rather than an over-engineered FOUC fix for a toggle this small.
    private void applyStoredTheme() {
        UI.getCurrent()
                .getPage()
                .executeJs(
                        "const theme = localStorage.getItem($0);"
                                + "if (theme === $1) { document.documentElement.setAttribute('theme',"
                                + " $1); } else { document.documentElement.removeAttribute('theme'); }",
                        THEME_STORAGE_KEY,
                        DARK_THEME);
    }

    private void toggleTheme() {
        UI.getCurrent()
                .getPage()
                .executeJs(
                        "const html = document.documentElement;"
                                + "if (html.getAttribute('theme') === $1) {"
                                + "  html.removeAttribute('theme');"
                                + "  localStorage.removeItem($0);"
                                + "} else {"
                                + "  html.setAttribute('theme', $1);"
                                + "  localStorage.setItem($0, $1);"
                                + "}",
                        THEME_STORAGE_KEY,
                        DARK_THEME);
    }
}
