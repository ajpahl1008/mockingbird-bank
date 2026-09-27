package com.mockingbirdbank.ui.view;

import com.mockingbirdbank.model.Account;
import com.mockingbirdbank.model.AccountHolder;
import com.mockingbirdbank.service.AccountService;
import com.mockingbirdbank.ui.layout.MainLayout;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.orderedlayout.FlexLayout;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.router.RouteAlias;

import java.math.BigDecimal;
import java.text.NumberFormat;
import java.util.List;
import java.util.Locale;

/**
 * The landing screen: total assets up top, then one card per account.
 * Mirrors the "Dashboard" artboard in the Mockingbird Bank design mockup.
 */
@Route(value = "accounts", layout = MainLayout.class)
@RouteAlias(value = "", layout = MainLayout.class)
public class DashboardView extends VerticalLayout {

    private static final NumberFormat USD = NumberFormat.getCurrencyInstance(Locale.US);

    public DashboardView(AccountService accountService) {
        AccountHolder holder = accountService.currentHolder();
        List<Account> accounts = accountService.accountsFor(holder);
        BigDecimal total = accountService.totalBalance(holder);

        setPadding(true);
        setSpacing(true);
        getStyle().set("background", "var(--mb-ivory)").set("min-height", "100%");

        add(balanceHero(holder, accounts, total));
        add(accountsSection(accounts));
    }

    private Div balanceHero(AccountHolder holder, List<Account> accounts, BigDecimal total) {
        Span label = eyebrow("Total assets");

        Span amount = new Span(USD.format(total));
        amount.getStyle()
                .set("font-family", "Georgia, serif")
                .set("font-size", "2.6rem")
                .set("font-weight", "600")
                .set("color", "var(--mb-navy)");

        Span sub = new Span("Across " + accounts.size() + " accounts");
        sub.getStyle().set("color", "var(--mb-text-muted)").set("font-size", "0.85rem");

        VerticalLayout left = new VerticalLayout(label, amount, sub);
        left.setPadding(false);
        left.setSpacing(false);

        Span holderLabel = eyebrow("Account holder");
        Span holderName = new Span(holder.getFullName());
        holderName.getStyle().set("font-weight", "600").set("color", "var(--mb-navy)");
        Span primaryAccount = holderPrimaryAccountLine(accounts);

        VerticalLayout right = new VerticalLayout(holderLabel, holderName, primaryAccount);
        right.setPadding(false);
        right.setSpacing(false);
        right.setAlignItems(FlexLayout.Alignment.END);

        HorizontalLayout hero = new HorizontalLayout(left, right);
        hero.setWidthFull();
        hero.setJustifyContentMode(FlexLayout.JustifyContentMode.BETWEEN);
        hero.setAlignItems(FlexLayout.Alignment.END);
        hero.getStyle()
                .set("background", "white")
                .set("border", "1px solid var(--mb-hairline)")
                .set("border-radius", "16px")
                .set("padding", "1.75rem 2.5rem");
        return new Div(hero);
    }

    private Span holderPrimaryAccountLine(List<Account> accounts) {
        String masked = accounts.stream()
                .findFirst()
                .map(Account::getMaskedNumber)
                .orElse("");
        Span line = new Span("Primary account " + masked);
        line.getStyle().set("color", "var(--mb-text-muted)").set("font-size", "0.85rem");
        return line;
    }

    private VerticalLayout accountsSection(List<Account> accounts) {
        H2 heading = new H2("Your accounts");
        heading.getStyle().set("font-family", "Georgia, serif").set("margin", "0");

        FlexLayout grid = new FlexLayout();
        grid.setFlexWrap(FlexLayout.FlexWrap.WRAP);
        grid.getStyle().set("gap", "1.25rem");
        accounts.forEach(account -> grid.add(new AccountCard(account)));

        VerticalLayout section = new VerticalLayout(heading, grid);
        section.setPadding(false);
        section.setSpacing(true);
        return section;
    }

    private Span eyebrow(String text) {
        Span span = new Span(text);
        span.getStyle()
                .set("font-size", "0.7rem")
                .set("letter-spacing", "0.08em")
                .set("text-transform", "uppercase")
                .set("color", "var(--mb-text-muted)")
                .set("font-weight", "600");
        return span;
    }
}
