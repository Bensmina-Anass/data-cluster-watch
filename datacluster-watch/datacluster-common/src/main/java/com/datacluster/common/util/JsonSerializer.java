package com.datacluster.common.util;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonParseException;

/**
 * Utilitaire de sérialisation / désérialisation JSON basé sur Gson.
 * L'instance {@link Gson} est partagée (thread-safe en lecture seule).
 */
public final class JsonSerializer {

    private static final Gson GSON = new GsonBuilder()
            .serializeNulls()
            .create();

    private JsonSerializer() {}

    /**
     * Sérialise un objet en chaîne JSON.
     *
     * @param object objet à sérialiser
     * @return représentation JSON
     */
    public static String toJson(Object object) {
        return GSON.toJson(object);
    }

    /**
     * Désérialise une chaîne JSON vers un objet du type donné.
     *
     * @param json  chaîne JSON source
     * @param clazz type cible
     * @param <T>   type générique
     * @return objet désérialisé
     * @throws JsonParseException si la chaîne n'est pas valide
     */
    public static <T> T fromJson(String json, Class<T> clazz) {
        return GSON.fromJson(json, clazz);
    }

    /**
     * Retourne l'instance Gson sous-jacente pour les cas d'usage avancés.
     *
     * @return instance {@link Gson}
     */
    public static Gson getGson() {
        return GSON;
    }
}
