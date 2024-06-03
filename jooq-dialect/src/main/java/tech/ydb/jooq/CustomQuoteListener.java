package tech.ydb.jooq;

import org.jooq.Name;
import org.jooq.QueryPart;
import org.jooq.RenderContext;
import org.jooq.VisitContext;
import org.jooq.VisitListener;

public class CustomQuoteListener implements VisitListener {

    private final String quote;

    public CustomQuoteListener(String quote) {
        this.quote = quote;
    }

    public CustomQuoteListener(char quote) {
        this.quote = String.valueOf(quote);
    }

    @Override
    public void visitStart(VisitContext context) {
        addQuoteForName(context);
    }

    @Override
    public void visitEnd(VisitContext context) {
        addQuoteForName(context);
    }

    private void addQuoteForName(VisitContext context) {
        QueryPart part = context.queryPart();
        if (part instanceof Name) {
            RenderContext renderContext = context.renderContext();
            if (renderContext != null) {
                renderContext.sql(quote);
            }
        }
    }
}

