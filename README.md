# AnnoHub. Language resource metadata for Linguistic Linked Open Data

Annohub (annotation hub) is a platform to create, harvest, host and provide language resource metadata as Linked Data. It is based on other metadata repositories from the language resource community (CLARIN VLO, individual CLARIN centers, META-SHARE) and exposes their data as Linked Data. In this regard, AnnoHub complements (and is imported into) LingHub, the primary metadata portal for the Linguistic Linked Open Data (LLOD) cloud, but provides additional routines to infer annotation metadata (language, formats, annotation schemas used) directly from the data, to curate, maintain and publish this data.

The Linguistic Linked Open Data (LLOD) cloud currently encompasses not only annotated corpora and dictionaries, but also terminological repositories, ontologies and knowledge bases. However, despite the efforts of improving interoperability and interconnection of resources by using semantic web vocabularies and technologies, many resources are still heterogeneously annotated and intransparently labeled with regard to their compatibility. This problem is by no means limited to LLOD but
applies to machine readable language resources in general.

Metadata repositories like META-SHARE, CLARIN centers or DataHub lack information about applicable annotation schemes. As for language metadata, language encoding standards vary across different metadata providers, and in addition metadata is not always provided as linked data.

Annohub tackles this deficit by creating a collection of languages and annotation schemes used in existing language resources, and thus aim to augment existing metadata repositories. Annohub therefore utilizes classification schemes supported by and linked to the thesaurus of the Bibliography of Linguistic Literature (BLL). This encompasses the Ontologies of Linguistic Annotation (OLiA) and its respective Linking Models for compatibility with a large amount of linguistic annotation schemes, and also Glottolog and lexvo for supported language identifiers.

Annhub has been developed by the Applied Computational Linguistics (ACoLi) Lab at Goethe-University Frankfurt, Germany, by the DFG/LIS project Fachinformationsdienst Linguistik, [linguistik.de](http://linguistik.de).
