package net.worldseed.multipart.mql;

import net.hollowcube.mql.jit.MqlEnv;

public interface MQLEvaluator {
    double evaluate(@MqlEnv({"q", "query"}) MQLData data);
}
