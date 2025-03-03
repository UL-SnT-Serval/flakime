package lu.uni.serval.flakime.core.instrumentation.models.vocabulary;

import java.util.Set;

public interface Model {
    enum Implementation {
        STANDFORD,
        WEKA
    }

    void setData(Data data, Set<String> additionalTrainingText);
    void train() throws Exception;
    double computeProbability(String body) throws Exception;
    double computeClass(String body) throws Exception;
    void save(String path) throws Exception;
}