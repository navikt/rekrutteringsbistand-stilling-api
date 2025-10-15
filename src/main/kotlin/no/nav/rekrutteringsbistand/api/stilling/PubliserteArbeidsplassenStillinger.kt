package no.nav.rekrutteringsbistand.api.stilling

import java.util.UUID

class PubliserteArbeidsplassenStillinger {
    companion object {
        /**
         * Nye stillinger skal publiseres via import-apiet, så dette er stillinger som allerede er publisert via Rest API'et
         */
        fun erPublisertPåArbeidsplassenViaRestApi(uuid: UUID): Boolean {
            return PUBLISERTE_STILLINGER.contains(uuid.toString())
        }

        val PUBLISERTE_STILLINGER: List<String> = listOf(
            // Fra produksjon
            "04ef9719-043a-4cf4-95f3-c0ca1255cadf",
            "18247203-9890-4654-887a-2c28c3230a9a",
            "18c37d2a-a0dd-4993-a1c3-16a8cc6ab339",
            "1cca2727-842c-4371-b4ed-b83705637ffe",
            "1d878d45-89dc-45da-8c8c-ef3d58965f47",
            "2107e470-55db-41e7-add0-f65f45e35b6c",
            "217124f3-9649-431e-90bd-fbc4140f7071",
            "2570a875-8e40-4d5f-92c2-310f51332757",
            "25e2259e-5850-4da6-b5b2-f8a5a39b503b",
            "2e7b6af5-dbfa-4226-97b5-351ac1166739",
            "2f6aa332-10f3-429d-99c1-d7915d935a11",
            "329439b7-29fa-450f-98a2-87ea2667a7b5",
            "3636199c-5634-49a3-9246-b6dcd147bf58",
            "3690db4e-f5e6-45e7-9e48-aad30abcc8b9",
            "38af1ea1-5ba0-4284-9864-7d317617f6c0",
            "3ef8f687-587c-42b6-a11b-a7fe49b6c153",
            "45473a73-05a0-40f8-a2fa-d8c50ce71a76",
            "4593e457-1bf4-4286-a825-47f6e94db942",
            "480e48fe-6f25-4eac-8107-7f578b3fa3df",
            "4a98bb54-12ff-49f9-b7f7-fa6841718aa6",
            "5571b56a-951b-4f6b-8bde-470da662f044",
            "5830cbe0-1e79-4ea2-8b2c-7875d5a4afad",
            "5d95e6a4-21c1-4ca7-a6cc-fbc94c8e578b",
            "5e1ca6d0-f915-4086-85bf-95e2bbee7f05",
            "69023925-d805-447b-8d1b-3ded2c117af3",
            "726c99b1-1e4f-4dfa-9572-09232ea18a47",
            "73a5a9c6-39e3-4172-a800-83492ece15b7",
            "7b65ca68-7b94-4300-a76d-7c8d128af31a",
            "7c9629c8-c8db-44b4-bb39-159c7b527493",
            "7f69e0fb-dbfd-4927-b8ab-536ef1972110",
            "8056498e-bde0-4cc3-aed4-de0dcee4063d",
            "8bd64f94-c7cb-4b63-a4a4-12ded3e27bb9",
            "8ec0ced5-86d3-4f58-971a-20fde264e92e",
            "956c9c3f-6782-4e79-8565-20178a24b033",
            "9f7ab69a-3a1f-4a8b-b647-a7d7730e8c98",
            "9fe9c78c-5b48-4c73-831a-ab4a070b0389",
            "a1f2c5ca-6021-4d70-95a9-6bba336ee218",
            "a615a079-6414-4997-9a15-97bc390ef854",
            "ad38effe-cf51-4147-bd62-989602b142bd",
            "afa77a42-bccb-4833-84d2-dd63a960f7c7",
            "aff528c2-8140-4cb1-989f-f362c1276eb8",
            "b02d6f18-bab8-462e-991b-8177b051acac",
            "c39d3d09-ddf7-4040-89eb-d00a878e85c6",
            "d25e40a4-3f7b-4449-a958-a37af5fadf0f",
            "d49f3fd8-6446-4e48-8c60-bd99058880b6",
            "d601a543-e0da-40eb-a7c9-7c0abfd3448e",
            "e44aa516-a175-44ff-963e-80a228333c18",
            "ec2ee3c2-4c0a-4774-9216-74667f0c762d",
            "ee4b87a6-8628-4bae-ae1e-e3484a862db5",
            "efc0a9f8-7d29-4813-b14f-cd0b64606b31",
            "f10ec29d-e890-4f45-8def-8942d170ede6",

            // Fra dev
            "94c0fd72-307f-4243-977e-db95de2cbb1c",
        )
    }

}
