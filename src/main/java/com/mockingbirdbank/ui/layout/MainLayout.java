package com.mockingbirdbank.ui.layout;

import com.mockingbirdbank.model.AccountHolder;
import com.mockingbirdbank.service.AccountService;
import com.vaadin.flow.component.applayout.AppLayout;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.html.H1;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.orderedlayout.FlexLayout;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.theme.lumo.LumoUtility;

/**
 * The dark top bar shared by every view: the bird wordmark on the left,
 * the signed-in holder's name and a (currently no-op) sign-out button on
 * the right. Individual views render everything below it.
 */
public class MainLayout extends AppLayout {

    public MainLayout(AccountService accountService) {
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

        Button signOut = new Button("Sign out");
        signOut.getElement().setAttribute("theme", "tertiary");
        signOut.getStyle().set("color", "var(--mb-ivory)").set("border", "1px solid #3C4E6E");

        HorizontalLayout right = new HorizontalLayout(greeting, signOut);
        right.setAlignItems(FlexLayout.Alignment.CENTER);
        right.setSpacing(true);

        HorizontalLayout bar = new HorizontalLayout(brand, right);
        bar.setWidthFull();
        bar.setAlignItems(FlexLayout.Alignment.CENTER);
        bar.setJustifyContentMode(FlexLayout.JustifyContentMode.BETWEEN);
        bar.addClassNames(LumoUtility.Padding.Horizontal.LARGE, LumoUtility.Padding.Vertical.MEDIUM);
        bar.getStyle().set("background", "var(--mb-navy)");

        addToNavbar(bar);
    }
}
