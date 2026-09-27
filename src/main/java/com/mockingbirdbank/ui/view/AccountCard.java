package com.mockingbirdbank.ui.view;

import com.mockingbirdbank.model.Account;
import com.mockingbirdbank.model.AccountType;
import com.vaadin.flow.component.html.Anchor;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.orderedlayout.FlexLayout;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;

import java.text.NumberFormat;
import java.util.Locale;

/**
 * One clickable account tile on the dashboard; the whole card links to
 * that account's transaction history.
 */
public class AccountCard extends Anchor {

    private static final NumberFormat USD = NumberFormat.getCurrencyInstance(Locale.US);

    public AccountCard(Account account) {
        super("accounts/" + account.getId(), "");
        getStyle()
                .set("display", "block")
                .set("text-decoration", "none")
                .set("color", "inherit")
                .set("background", "white")
                .set("border", "1px solid var(--mb-hairline)")
                .set("border-radius", "12px")
                .set("padding", "1.4rem 1.5rem")
                .set("width", "280px")
                .set("box-shadow", "0 1px 2px rgba(18,35,63,0.05)");

        Span name = new Span(account.getDisplayName());
        name.getStyle().set("font-weight", "600").set("color", "var(--mb-navy)");

        Span tag = new Span(account.getAccountType() == AccountType.CHECKING ? "Checking" : "Savings");
        tag.getStyle()
                .set("font-size", "0.68rem")
                .set("font-weight", "600")
                .set("letter-spacing", "0.04em")
                .set("text-transform", "uppercase")
                .set("padding", "3px 9px")
                .set("border-radius", "999px")
                .set("color", account.getAccountType() == AccountType.CHECKING ? "#1F6F63" : "#7A5A1F")
                .set("background", account.getAccountType() == AccountType.CHECKING ? "#E7F1EE" : "#F3EBDD");

        HorizontalLayout header = new HorizontalLayout(name, tag);
        header.setWidthFull();
        header.setJustifyContentMode(FlexLayout.JustifyContentMode.BETWEEN);
        header.setAlignItems(FlexLayout.Alignment.CENTER);

        Span number = new Span(account.getMaskedNumber());
        number.getStyle().set("color", "var(--mb-text-muted)").set("font-size", "0.8rem");

        Span balance = new Span(USD.format(account.getBalance()));
        balance.getStyle()
                .set("font-family", "Georgia, serif")
                .set("font-size", "1.6rem")
                .set("font-weight", "600")
                .set("color", "var(--mb-navy)")
                .set("margin-top", "0.75rem");

        Span link = new Span("View transactions →");
        link.getStyle().set("color", "#1F6F63").set("font-size", "0.8rem").set("font-weight", "500")
                .set("margin-top", "0.85rem");

        VerticalLayout body = new VerticalLayout(header, number, balance, link);
        body.setPadding(false);
        body.setSpacing(false);
        add(body);
    }
}
