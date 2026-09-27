package com.mockingbirdbank.ui.layout;

import com.vaadin.flow.component.Html;

/**
 * The Mockingbird Bank glyph, matching the mark used in the dashboard
 * mockup: an original bird silhouette, drawn as inline SVG rather than an
 * image asset so it stays crisp on the dark top bar at any size.
 */
public class BirdMark extends Html {

    public BirdMark() {
        super("<span style='display:inline-flex'>"
                + "<svg width='30' height='30' viewBox='0 0 48 48' aria-hidden='true'>"
                + "<path d='M8 34 C 8 34, 14 28, 24 28 C 34 28, 42 22, 44 14 "
                + "C 40 16, 36 16, 33 14 C 36 12, 37 9, 37 6 C 33 9, 30 10, 27 9 "
                + "C 22 7, 16 9, 13 14 C 10 18, 10 22, 12 25 C 8 26, 6 29, 6 33 Z' "
                + "fill='#EDE7D3'/>"
                + "<circle cx='31' cy='12' r='1.6' fill='#12233F'/>"
                + "</svg></span>");
    }
}
