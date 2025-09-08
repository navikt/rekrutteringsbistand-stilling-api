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
            "6e995259-ab3a-4c38-88cb-47c08839bc01",
            "2ff2f9e0-c685-4311-9264-55008c9cac32",
            "36749460-01c2-4f3b-af60-cb09609e0c30",
            "9fcae0bb-63b8-4897-a60a-e78fccb93b1f",
            "bf4e515d-cdb9-4fc2-bc87-79046f4e6d23",
            "217124f3-9649-431e-90bd-fbc4140f7071",
            "9f7ab69a-3a1f-4a8b-b647-a7d7730e8c98",
            "dc806389-1fa5-4d36-b901-221cf78c5780",
            "5e1ca6d0-f915-4086-85bf-95e2bbee7f05",
            "9fe9c78c-5b48-4c73-831a-ab4a070b0389",
            "ace9ed0c-639c-4c2e-9baa-1b3b4a66c8e9",
            "013eb70f-b159-40d9-816f-3286dac851a2",
            "dbfa4cb7-1086-4e02-bb93-8c4934897247",
            "a9d431c1-ee8f-471e-a8c4-149e8ba9e72b",
            "e853d82b-855b-4e60-97a7-2b66055fcf80",
            "862037b3-0cff-4f9f-8f16-85e65f6517a3",
            "0d8da01b-cb4d-41de-919f-416326a7e97a",
            "25e2259e-5850-4da6-b5b2-f8a5a39b503b",
            "329439b7-29fa-450f-98a2-87ea2667a7b5",
            "6dfd1d8d-1382-4ccf-b7d6-d696267ec61a",
            "2570a875-8e40-4d5f-92c2-310f51332757",
            "1d878d45-89dc-45da-8c8c-ef3d58965f47",
            "edeb93dc-9f29-4524-b3c6-685cf986e1dc",
            "1800c7c4-e16c-466c-8ea9-5e8c0938127f",
            "f0ffb59c-0f4d-4fe3-b17c-1cf926f1265f",
            "422534ab-774d-4404-9b98-1c3a6f6e9833",
            "bd51d067-70c7-4ff7-b463-fb163e7ac3b3",
            "0351e695-55d3-492d-978f-554fae09af52",
            "726c99b1-1e4f-4dfa-9572-09232ea18a47",
            "5f31beb9-6007-46c7-8f01-c2f3864fb90a",
            "8cb0e6aa-b22c-4f22-95fc-1875285543bc",
            "efcd9a48-efd8-44ac-b2c8-e57a311e63ed",
            "f620fb4f-6610-40d1-bbb3-8abe3f70732a",
            "35ecb15e-fb77-4c19-80e0-a45da91dd52d",
            "877778b8-2bd4-4f00-bc15-b405d76f5bd7",
            "8ec0ced5-86d3-4f58-971a-20fde264e92e",
            "a48d2565-5bf2-4c32-8b90-caa272a0c2c3",
            "6dc3e9d2-8fc9-4c29-ae74-6b99277e810b",
            "e5988055-6b31-486a-9539-5b3a7912f732",
            "591391d1-02fb-4fe1-9c24-44405fd0c18d",
            "afa77a42-bccb-4833-84d2-dd63a960f7c7",
            "1d423ad8-af3c-4802-9114-233f3ef23d7e",
            "f598070f-4776-4db7-a304-9687da2bac41",
            "ea8e5de1-ced0-4781-a946-49e8222d3855",
            "3636199c-5634-49a3-9246-b6dcd147bf58",
            "cd5b5333-2362-4776-baf9-c14a9129149e",
            "ec8b93d8-43b5-4299-bc50-31820a96236f",
            "5571b56a-951b-4f6b-8bde-470da662f044",
            "6bd9c1f8-139e-46d1-b9fe-2dc314652564",
            "68552d1b-59dd-49d9-b1b4-5cbc3a102f7d",

            // Fra dev
            "94c0fd72-307f-4243-977e-db95de2cbb1c",
        )
    }

}
