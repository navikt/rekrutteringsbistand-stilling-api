package no.nav.rekrutteringsbistand.api.data

data class ElasticSearchResult(
        val took: Int,
        val timed_out: Boolean,
        val _shards: ElasticSearchResultShards,
        val hits: ElasticSearchHits
);

data class ElasticSearchResultShards(
        val total: Int,
        val successful: Int,
        val skipped: Int,
        val failed: Int
);

data class ElasticSearchHits(
        val total: ElasticSearchHitsTotal,
        val max_score: Float,
        val hits: List<ElasticSearchHit> = ArrayList()
);

data class ElasticSearchHitsTotal(
        val value: Int,
        val relation: String
);

data class ElasticSearchHit(
        val _index: String,
        val _type: String,
        val _id: String,
        val _score: Float,
        val _source: ElasticSearchHitSource
);

data class ElasticSearchHitSource(
        val organisasjonsnummer: String,
        val navn: String,
        val organisasjonsform: String,
        val antallAnsatte: Int,
        val overordnetEnhet: String,
        val adresse: ElasticSearchHitSourceAddresse,
        val naringskoder: List<ElasticSearchHitSourceNæringskode> = ArrayList()
);

data class ElasticSearchHitSourceAddresse(
        val adresse: String,
        val postnummer: String,
        val poststed: String,
        val kommunenummer: String,
        val kommune: String,
        val landkode: String,
        val land: String
);

data class ElasticSearchHitSourceNæringskode(
        val kode: String,
        val beskrivelse: String
);
