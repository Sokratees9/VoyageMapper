package org.okane.voyagemapper.util;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;
import org.okane.voyagemapper.model.SeeListing;

import java.util.Arrays;
import java.util.List;

import io.hosuaby.inject.resources.junit.jupiter.GivenTextResource;
import io.hosuaby.inject.resources.junit.jupiter.TestWithResources;

@TestWithResources
class TemplateMatcherTest {

    @Test
    public void parsesSingleLineMarkerSee() {
        String wikitext = "* {{marker | name=Lochrin Basin | type=see | lat=55.9427 | long=-3.2081 }} is the east terminus.";

        List<SeeListing> out = TemplateMatcher.parse(wikitext); // whatever your class is called
        assertEquals(1, out.size());

        SeeListing s = out.get(0);
        assertEquals("Lochrin Basin", s.name());
        assertEquals(55.9427, s.lat(), 1e-6);
        assertEquals(-3.2081, s.lon(), 1e-6);
        // tail content comes from the prose after }}
        assertTrue(s.content().contains("east terminus"));
    }

    @Test
    public void parsesSingleLineListingSee() {
        String wikitext =
                """
                        * {{listing | type=see
                        | name=Octagon House | alt=Loren Andrus Octagon House | url=http://www.octagonhouse.org/ | email=
                        | address=57500 Van Dyke | lat=42.718333 | long=-83.035 | directions=
                        | phone=+1 586 781-0084 | tollfree=
                        | hours= | price=
                        | wikidata=Q6680344
                        | content=An icon of early history, capturing attention with its unusual symmetry and serving as a metaphor for a community that bridges yesterday and tomorrow. It was used as a safehouse on the [[Underground Railroad]].
                        }}
                """;

        List<SeeListing> out = TemplateMatcher.parse(wikitext); // whatever your class is called
        assertEquals(1, out.size());

        SeeListing s = out.get(0);
        assertEquals("Octagon House", s.name());
        assertEquals(42.718333, s.lat(), 1e-6);
        assertEquals(-83.035, s.lon(), 1e-6);
        assertTrue(s.content().contains("of early history, capturing attention with its unusual"));
    }

    @Test
    public void parsesMultilineSee_LauristonCastle() {
        String wikitext =
                """
                        * {{see
                        | name=Lauriston Castle | alt= | url=https://www.edinburghmuseums.org.uk/venue/lauriston-castle | email=
                        | address=2 Cramond Road South EH4 6AD | lat=55.9712 | long=-3.2784 | directions=
                        | phone=+44 131 336-2060 | tollfree= | hours=Sa Su 11AM-3PM | price=Tour adult £8, conc £6
                        | wikipedia=Lauriston Castle | wikidata=Q2969689 | lastedit=2023-12-17\s
                        | content=This 1590s tower house was extended in the 1820s and housed a series of prosperous families.
                        }}
                """;

        List<SeeListing> out = TemplateMatcher.parse(wikitext);
        assertEquals(1, out.size());

        SeeListing s = out.get(0);
        assertEquals("Lauriston Castle", s.name());
        assertEquals(55.9712, s.lat(), 1e-6);
        assertEquals(-3.2784, s.lon(), 1e-6);
        assertTrue(s.content().startsWith("This 1590s tower house"));
    }

    @Test
    public void skipsMarkerWithoutSeeOrDoType() {
        String wikitext =
                "* {{marker | name=Random Hotel | type=sleep | lat=55.0 | long=-3.0 }} somewhere.\n";

        List<SeeListing> out = TemplateMatcher.parse(wikitext);
        assertTrue(out.isEmpty());
    }

    @Test
    public void parsePortarlington(
            @GivenTextResource("Portarlington_full_article.txt") String portarlington) {

        SeeListing marketSquare = new SeeListing("Market Square", 53.162, -7.190, null, null,
                "is the town centre, with 18th- and 19th-century low-rise in the streets around. The Huguenot tradition really only lives on in the name French Church Street, referring to St Paul's (Church of Ireland). Services were held in French in its predecessor into the 1820s, but the present church was built over the site in 1851. St Michael's is the Roman Catholic church, west on Patrick St.",
                null, null, null, null, null, null);
        SeeListing carrickWood = new SeeListing("Carrick Wood", 53.144, -7.188, null, null,
                "is on a small hill south side of town. Stroll up from the car park to \"The Spire\", an 18th-century folly probably made from the ruin of a windmill. The town waterworks are adjacent.",
                null, null, null, null, null, null);
        SeeListing leaCastle = new SeeListing("Lea Castle", 53.156, -7.147, null, null,
                "is a ruin along R420 four km east of town. It was built in fits and starts over the 13th century, replacing a timber and earthwork structure, and in its pomp was a mighty four-towered donjon or keep with an inner and outer moat and bawn wall. But it was wrecked on multiple occasions, and was already in a sorry state when Cromwell's troops came by in 1650, and wrecked it some more just to keep their hands in. One last tenant lived here until 1737. The ruin is overgrown and unsafe, so it's closed to the public, and work is under way to try to stabilise what remains.",
                null, null, null, null, null, null);
        SeeListing emoCourt = new SeeListing("Emo Court", 53.107, -7.197, "+353 57 862 6573", "https://emocourt.ie/",
                "Grand neo-classical mansion, designed by Gandon in 1790 but construction took another 70 years. Then at last the Earls of Portarlington could enjoy it and entertain in glittering style, but these were the final halcyon days of the Protestant ascendancy. Arms Quarterly 1st & 4th, Barry nebuly of six Argent and Gules, over all a Bend engrailed Azure... (and so on, at some length) somehow didn't enthrall an independent Republic of Ireland. The family left, and the house lay empty until 1930, then was a Jesuit college until 1969. Not wishing to sully their pious gaze with Grecian nymphs, the Jesuits chucked the statues in the lake. The estate was bought by Major Cholmeley Harrison, who restored the house, fished out the statues and opened the gardens to the public. House and gardens are now curated by the Office of Public Works, but the house is closed for restoration until summer 2024.",
                "Emo R32 C44V", "Grounds daily 8:30AM-4:30PM", null, null, null, null);

        List<SeeListing> things = TemplateMatcher.parse(portarlington);
        assertEquals(5, things.size());
        assertEquals(marketSquare, things.get(0));
        assertEquals(carrickWood, things.get(1));
        assertEquals(leaCastle, things.get(2));
        assertEquals(emoCourt, things.get(3));
    }

    @Test
    public void testBarcelonaEixampleSee(
            @GivenTextResource("Barcelona_eixample_see.txt") String eixample) {

        SeeListing sagradaFamilia = new SeeListing(
                "Sagrada Família",
                41.403608,
                2.174442,
                "+34 932 080 414",
                "https://sagradafamilia.org/en/home",
                "Barcelona's most popular and famous attraction, the Sagrada Família is a testament to Antonio Gaudi's modernist style and unlike any other church in the world. The texture and detail on the façades are intricate and incorporate forms seen in nature as much of Gaudi's work does. The inside is huge — it's estimated that it can hold 13,000 people — with vaulted ceilings and columns designed to be reminiscent of trees. The church is still under construction, but the central nave, apse and crypt are open. Seeing the Sagrada Família at night with lights on is impressive — this is the time when you understand why people say that it is built of bones.",
                "Carrer de Mallorca, 401",
                "Daily: Nov-Mar 09:00-18:00; Oct Mar 09:00-19:00; Apr-Sep 09:00-20:00",
                "€26 for adult, with audio guide, €30 if tour included. €10 extra to visit the towers",
                "Sagrada Família",
                "Q48435",
                "Sagradafamilia-overview.jpg"
        );
        SeeListing casaBatllo = new SeeListing(
                "Casa Batlló",
                41.3916384,
                2.16476979,
                "+34 932 16 03 06",
                "https://www.casabatllo.es/en/",
                "Another Gaudi masterpiece along the same avenue as La Pedrera. Open to the public. No straight line or right angles appear anywhere in the house. Often overlooked for La Pedrera, (also by Gaudi), La Casa Batlló is equally as stunning with its unique architecture and infamous two ornamental pillars in the entrance to the terrace. Open for visitors from ground floor to the roof, and down by an old-time elevator.",
                "Passeig de Gracia 43",
                "09:00-22:00 Last entry 20:45",
                "Adult €29-€45 (online) discounts for residents, children, students & seniors. demand led pricing.Tickets at counter are €4 more expensive.",
                "Casa Batlló",
                "Q461371",
                "CasaBatllo 0055.JPG"
        );
        SeeListing laPedrera = new SeeListing(
                "La Pedrera",
                41.395278,
                2.161667,
                "+34 932 142 576",
                "https://www.lapedrera.com/en",
                "One of Gaudi's masterpieces, there is not one straight edge on the exterior and the structure is one of Gaudi’s most eccentric and enticing architectural creations. Tours of the interior and the incredible roof structures are available. Great views of the tall buildings (especially La Sagrada Familia) from the roof. Hosts a large exposition of Gaudi works, covering Sagrada Familia and Casa Batlio, not only La Pedrera itself. The exposition is a good place to start your exploration of Gaudi; it reveals many hidden details for the art novice.",
                "Passeig de Gràcia 92",
                "09:00-20:30",
                "from €29, children 0-6 free, discounts for older children, students & 65+. €2 additional charge if buying from ticket office",
                "Casa Milà",
                "Q207870",
                "Casa Milà - Barcelona, Spain - Jan 2007.jpg"
        );
        SeeListing casaAmatller = new SeeListing(
                "Casa Amatller",
                41.391667,
                2.165,
                "+34 934 617 460",
                "http://amatller.org/en/",
                "Designed by Puig i Cadafalch, the building is a fine work of Modernism. Check the detailed photos and explanation of facade sculptures. It also has a shop that sells fine chocolate (the Amatller family made its money out of cocoa).",
                "Passeig de Gràcia 41",
                "Daily 10:00-19:00 only every 1/2 hr (in English at 11:00); no high heels allowed",
                "Audio-guided tour €17 reduced €15: Guided tour €20 reduced €17 (online prices)",
                "Casa Amatller",
                "Q506814",
                "Casa Amatller - Barcelona 2014.jpg"
        );
        SeeListing casaLleoMorera = new SeeListing(
                "Casa Lleó-Morera",
                41.391244,
                2.165528,
                "+34 936 762 733",
                "http://www.casalleomorera.com/",
                "By Lluís Domènech i Montaner.",
                "Passeig de Gràcia 35",
                "Tu-Su 10:00-13:30, 15:00-19:00 (only guided tours roughly every 1/2 hr, full tour in English at 11:00)",
                "Adult €15 (sold online or at [http://ajuntament.barcelona.cat/lavirreina/en/ Palau de la Virreina] (La Rambla, 99)",
                "Casa Lleó Morera",
                "Q2330791",
                "CasaLleoMorera-Full.jpg"
        );
        SeeListing fundacioAntoniTapies = new SeeListing(
                "Fundació Antoni Tàpies",
                41.39167,
                2.16361,
                null,
                "https://fundaciotapies.org/en/",
                "Created in 1984 by the artist Antoni Tàpies to promote the study and knowledge of modern and contemporary art.",
                "C/d'Aragó, 255",
                null,
                null,
                "Fundació Antoni Tàpies",
                "Q2447148",
                "Domènech.i.Montaner.Editorial.Montaner.i.Simón.1.Barcelona.JPG"
        );
        SeeListing hotelMajestic = new SeeListing(
                "Hotel Majestic",
                41.393406,
                2.163947,
                "+34 934 881 717",
                "http://www.hotelmajestic.es/",
                "Passeig de Gràcia 68. Enjoy city views from the rooftop. Just enter the elevator right from the lobby, and head up to the top floor.",
                null,
                null,
                null,
                "Hotel Majestic (Barcelona)",
                "Q615575",
                "Hotel Majestic Barcelona.JPG"
        );
        SeeListing placaDeCatalunya = new SeeListing(
                "Plaça de Catalunya",
                41.386667,
                2.17,
                null,
                null,
                "Connecting many of the major streets in the city, the Plaça is a central location and a hub of activity. It is a favourite meeting spot for locals and a transit hub for the tourist buses. There are many fountains, statues and gardens around the plaza, and a number of benches to just sit and take it all in.",
                "at the end of Passeig de Gràcia and the top of La Rambla",
                null,
                null,
                "Plaça de Catalunya",
                "Q822881",
                "050529 Barcelona 109.jpg"
        );
        SeeListing universitat = new SeeListing(
                "Universitat",
                41.386667,
                2.163889,
                null,
                "http://www.ub.cat",
                "The university has many interesting buildings, libraries and courtyards.",
                null,
                null,
                null,
                "University of Barcelona",
                "Q219615",
                "PlacaUniversitat.jpg"
        );

        List<SeeListing> expected = Arrays.asList(
                sagradaFamilia,
                casaBatllo,
                laPedrera,
                casaAmatller,
                casaLleoMorera,
                fundacioAntoniTapies,
                hotelMajestic,
                placaDeCatalunya,
                universitat
        );
        List<SeeListing> parse = TemplateMatcher.parse(eixample);
        assertEquals(expected.size(), parse.size());
        assertEquals(expected, parse);
    }

    @Test
    public void testMunichCentreSee(
            @GivenTextResource("Munich_centre_see.txt") String munich) {
        SeeListing altesRathaus = new SeeListing(
                "Altes Rathaus & Spielzeugmuseum",
                null,
                null,
                "+49 89 294001",
                "https://www.spielzeugmuseummuenchen.de/",
                "This was built in 1474 to replace an even older town hall, and was the city chambers until Neues Rathaus was built in the late 19th century. It was bombed during World War II but rebuilt. It's now mostly a ceremonial hall and events venue, but the tower contains a toy museum.",
                "Marienplatz 15",
                "Daily 10:00-17:30",
                "Museum adult €6, child €2",
                "Old Town Hall, Munich",
                "Q439458",
                "Altes Rathaus in München Ostseite.jpg"
        );
        SeeListing neuesRathaus = new SeeListing(
                "Neues Rathaus",
                null,
                null,
                null,
                null,
                "This was built in phases from 1867 to 1909. It's still a busy city chambers, with access only on business, but you can ride the lift to the observation deck of the tower. For most visitors the ornate neo-Gothic exterior is experience enough. At 11:00, 12:00 and 17:00 the Glockenspiel cranks into life: medieval minstrels parade, knights joust (the knight in Bavarian blue always wins) and Morris dancers pirouette to the sound of the bells. They all freeze en tableaux to the tune of \"I don't have a wooden heart\" (Elvis Presley, 1960) which alas they do.",
                "Marienplatz 8",
                "Daily 10:00-18:00",
                "Tower €6",
                "New Town Hall (Munich)",
                "Q152979",
                "Rathaus and Marienplatz from Peterskirche - August 2006.jpg"
        );
        SeeListing peterskirche = new SeeListing(
                "Peterskirche",
                null,
                null,
                null,
                "https://alterpeter.de/pfarrkirche-st-peter/",
                "Munich's first parish church was started in the 12th century, but burned down in the great city fire of 1327. It was rebuilt by 1368 in Gothic style. The 92 m main tower (called Alter Peter) stands between the stumps of two original towers. The interior is basilica plan, with an elaborate gilded high altar, richly decorated side chapels and artwork. Roman Catholic mass is held Tu-Su at 10:00, 17:15 & 18:00. You can climb the 306 steps of the tower for city views.",
                "Rindermarkt 1",
                "Daily 07:30-19:00",
                "Tower adult €5, conc €3, child €2",
                "St Peter's Church (Munich)",
                "Q707645",
                "Alter Peter und Rindermarkt.jpg"
        );
        SeeListing frauenkirche = new SeeListing(
                "Frauenkirche",
                48.1386,
                11.5739,
                "+49 89 290 0820",
                "https://www.muenchner-dom.de/",
                "This huge church defines the Munich skyline with its twin onion-domed towers, a shade over 98 m tall - the money ran out for the planned spires. It's the Roman Catholic cathedral, but always known locally as Frauenkirche. It was built from 1469 to 1488 in brick because of a lack of suitable local stone; the towers were only completed in 1525, cleverly anticipating a law of 2004 that no structure within Altstadt may exceed 99 m in height. Although its style is Gothic, it's fairly simple outside and in, without the usual ornamentation of that style. Eleven kings or archdukes of Bavaria are interred here. The church was bombed in wartime and full restoration took until 1994. Its three naves are of equal height, creating a single hall soaring so high that you can't see the windows, and light floods in from no visible source. This is in legend is the feature that caused the devil to stamp in pique, leaving a black footprint (Teufelstritt) within a yellow flagstone just inside the entrance. You can climb the south tower.",
                "Frauenplatz",
                "Daily 07:30-20:30, tower ascent 10:00-17:00",
                "Free; tower ascent adult €7.50, child €5.50",
                "Munich Frauenkirche",
                "Q167193",
                "Frauenkirche Munich - View from Peterskirche Tower.jpg"
        );
        SeeListing viktualienmarkt = new SeeListing(
                "Viktualienmarkt",
                48.1354,
                11.5762,
                null,
                null,
                "is the pedestrianised street and outdoor market running south from Marienplatz opposite Old Town Hall.",
                null,
                null,
                null,
                null,
                null,
                null
        );
        SeeListing theatinerkirche = new SeeListing(
                "Theatinerkirche",
                null,
                null,
                null,
                "http://www.theatinerkirche.de/",
                "Roman Catholic church in eye-catching Italianate High Baroque, looking like it belongs in Syracuse. The Theatins were an austere monastic order founded by the 15th century Saint Cajetan, and they built several similar churches. This one was built from 1662 as thanksgiving for the birth of Maximilian II Emanuel, which secured the royal succession. The church was damaged in wartime but restored, and is now in the care of the Dominican Order.",
                "Odeonsplatz",
                "Daily 07:00-20:00",
                "Free",
                "Theatine Church, Munich",
                "Q259799",
                "Exterior of Theatinerkirche, Munich.jpg"
        );
        SeeListing kunsthalle = new SeeListing(
                "Kunsthalle München der Hypo Kulturstiftung",
                48.1399,
                11.5759,
                "+49 89 224412",
                "https://www.kunsthalle-muc.de/",
                "Modern art museum, no permanent collection but rotating exhibitions. It's funded by HypoVereinsbank.",
                "Theatinerstraße 8",
                "Daily 10:00-20:00",
                "Adult €15",
                "de:Kunsthalle der Hypo-Kulturstiftung",
                "Q1367890",
                "Kunsthalle der Hypo-Kulturstiftung.jpg"
        );
        SeeListing residenz = new SeeListing(
                "Residenz",
                48.1411,
                11.5781,
                "+49 89 290 671",
                "https://www.residenz-muenchen.de/",
                "Royal Palace and seat of government of the Wittelsbach dynasty, bombed in wartime but rebuilt. The two principal areas are the Treasury and the Museum (separate admission €9 each). The Treasury (Schatzkammer) has a huge collection of ecclesiastical and secular bling, such as the royal crowns, orb and sceptre. In the Museum, the Antiquarium is the barrel-vaulted banqueting hall. The Papal Rooms accommodated Pius VI on his visit to Munich and are Baroque. The Ancestral Gallery, Porcelain Cabinet and Ornate Rooms are Rococo. The neo-classical Königsbau, added in the 19th century, houses the Charlotte Rooms, the royal apartments and the Halls of Battles. There's an additional charge for the Cuvilliés Theatre.",
                "Residenzstraße 1",
                "Daily Apr-15 Oct 09:00-18:00, 16 Oct-Mar 10:00-16:00",
                "Combi adult €14, conc €12",
                "Munich Residenz",
                "Q686548",
                "Münchner Residenz 2014-08-02 Pano.jpg"
        );
        SeeListing hofgartenMarker = new SeeListing(
                "Hofgarten",
                48.1428,
                11.580,
                null,
                null,
                "is a stately Italianate garden laid out from 1613 as part of the Residenz development. The main entrance is west opposite the U-Bahn station. Most of its embellishments were lost to wartime bombing but the central statue of Diana is original. The garden is free to enter 24 hours and has a pleasant cafe. A pedestrian tunnel links north to the English Garden.",
                null,
                null,
                null,
                null,
                null,
                null
        );
        SeeListing deutschesTheatermuseum = new SeeListing(
                "Deutsches Theatermuseum",
                null,
                null,
                "+49 89 210 6910",
                "http://www.deutschestheatermuseum.de",
                "This museum about German Theatre remains closed in 2022. Some exhibits are on temporary display elsewhere in the city.",
                "Galeriestraße 4a",
                "Closed",
                null,
                "Deutsches Theatermuseum",
                "Q1206066",
                "München Theatermuseum (Schild).JPG"
        );
        SeeListing stateChancellery = new SeeListing(
                "State Chancellery",
                48.1424,
                11.5828,
                null,
                "https://www.bayern.de/staatskanzlei/aufgaben-und-organisation/",
                "This fills the east side of Hofgarten, housing the Bavarian Minister (president of the Lande government) and his / her cabinet meetings. The central dome was built in 1879 to house the Royal Bavarian Army Museum. It was bombed out in World War II and only restored in the 1980s. The modern side wings were built 1989-93. In front of the building is a memorial to Munich citizens who died in wartime, and a statue of Duke Otto I, 12th century founder of the Bavarian royal dynasty. Admission to the Chancellery is only on business but their website gives an online tour.",
                "Franz-Josef-Strauß-Ring 1",
                null,
                null,
                "Bayerische Staatskanzlei",
                "Q812286",
                "Bayerische Staatskanzlei, München, Deutschland04.jpg"
        );
        SeeListing hausDerKunst = new SeeListing(
                "Haus der Kunst",
                48.1442,
                11.5858,
                "+49 89 211 27113",
                "https://www.hausderkunst.de",
                "Grand exhibition hall completed in 1937 in Nazi - Art Deco style. It doesn't have a permanent collection but mounts ever-changing graphic arts exhibitions.",
                "Prinzregentenstraße 1",
                "W-M 10:00-20:00",
                null,
                "Haus der Kunst",
                "Q697782",
                "Haus der Kunst - Munich - 2013.jpg"
        );
        SeeListing monopteros = new SeeListing(
                "Monopteros",
                48.1499,
                11.5909,
                null,
                null,
                "is the focal point of this section, a prominent neo-classical rotunda. 200 m further north is the Chinese Tower, with a beer garden, and site of a Christmas Market. The section is bounded by a busy highway, Isarring. The northern section is more easily accessed from U-bahn U6 Nordfriedhof.",
                null,
                null,
                null,
                null,
                null,
                null
        );
        SeeListing bayerischesNationalmuseum = new SeeListing(
                "Bayerisches Nationalmuseum",
                48.1432,
                11.5910,
                "+49 89 211 2401",
                "https://www.bayerisches-nationalmuseum.de/",
                "Huge and fascinating museum of European history from the Middle Ages until early 20th century. There's armour, pottery, furniture, fashions, porcelain, beer mugs, the lot; plus Oct-April a display of nativity scenes. The Bollert medieval art collection is only open Th F 10:00-17:00.",
                "Prinzregentenstraße 3",
                "Tu W F-Su 10:00-17:00, Th 10:00-20:00",
                "Adult €7, conc €6; Sunday €1; child free",
                "Bavarian National Museum",
                "Q631056",
                "Bayerisches Nationalmuseum - Muenchen - 2013.jpg"
        );
        SeeListing schackGallery = new SeeListing(
                "Schack Gallery",
                48.1423,
                11.5934,
                "+49 89 238 05224",
                "https://www.pinakothek.de/besuch/sammlung-schack",
                "An extensive private collection of 19th century Late Romantic art.",
                "Prinzregentenstraße 9",
                "W-Su 10:00-18:00",
                "Adult €4, conc or child €3",
                "Schackgalerie",
                "Q523652",
                "München Schack-Galerie.JPG"
        );
        SeeListing museumFuenfKontinente = new SeeListing(
                "Museum Fünf Kontinente",
                48.1376,
                11.5856,
                "+49 89 210 136100",
                "https://www.museum-fuenf-kontinente.de/",
                "Omitting Europe and Antarctica, the former Museum für Völkerkunde is a global ethnological collection.",
                "Maximilianstraße 42",
                "Tu-Su 09:30-17:30",
                "Adult €4, conc or child €3, Sunday €1",
                null,
                null,
                null
        );
        SeeListing kunstfoyerVkb = new SeeListing(
                "Kunstfoyer VKB",
                48.1376,
                11.5900,
                "+49 89 21600",
                "https://www.versicherungskammer-kulturstiftung.de/kunstfoyer/",
                "Changing art exhibitions provided by the art foundation of Versicherungskammer Bayern.",
                "Maximilianstraße 53",
                "Daily 09:30-19:00",
                "Free",
                null,
                null,
                null
        );
        SeeListing praterIsland = new SeeListing(
                "Prater island",
                48.136,
                11.590,
                null,
                null,
                "is one of two large river islands in the Isar. Originally these were small, shifting sandbanks, then in the 19th century they were consolidated and developed. A tavern opened, with a funfair inspired by Prater in Vienna, and so the island (a fusion of three) got its name. But Orson Welles never got to ride its carousel, and the funfair went bust through disruption of building Maximilianstraße, which now bisects it and connects to the eastern district of Haidhausen. One fellow wanted to experiment with fireworks was ordered to live away at the south tip, thus dubbed Feuerwerkinsel. This tip is linked by footbridge to Mariannenplatz in Lehel, to Haidhausen, and upriver to Museum Island, described as part of Ludwigsvordstadt",
                null,
                null,
                null,
                null,
                null,
                null
        );
        SeeListing isartor = new SeeListing(
                "Isartor",
                48.1350,
                11.5818,
                null,
                null,
                "(S-Bahn: Isartor) was built in 1337 as one of the city gates. It was rebuilt in the 19th to the original plan. It marks the natural boundary of Altstadt-Lehel: for sights south and east such as the Deutsches Museum, see Ludwigsvorstadt.",
                null,
                null,
                null,
                null,
                null,
                null
        );
        SeeListing maximiliansplatzMarker = new SeeListing(
                "Maximiliansplatz",
                48.1419,
                11.5711,
                null,
                null,
                "is a long plaza laid out at the beginning of the 19th century over the former city walls. Those circled west to Karlsplatz and east to Odeonsplatz, bounding the northwest segment of Altstadt. Within the plaza are the Wittelsbacherbrunnen fountain, the New Stock Exchange (Neue Börse) of 1901 and the House of Artists (Künstlerhaus). It's a lively place at night. U-Bahn Karlsplatz (Stachus) is closest for the west end, Odeonsplatz for the east end, and Tram 19 (stop Lenbachplatz) runs through it.",
                null,
                null,
                null,
                null,
                null,
                null
        );
        SeeListing briennerStrasse = new SeeListing(
                "Brienner Straße",
                48.1430,
                11.5755,
                null,
                null,
                "is the short boulevard connecting Maximiliansplatz to Odeonsplatz. It's lined with neo-classical bulidings, with the grandest in Wittelsbacherplatz.",
                null,
                null,
                null,
                null,
                null,
                null
        );
        SeeListing alpineMuseum = new SeeListing(
                "Alpine Museum",
                null,
                null,
                "+49 89 211 2240",
                "https://www.alpenverein.de/Kultur/",
                "A small museum displaying the history of alpine exploration in the Bavarian Alps. It remains closed in 2022.",
                "Praterinsel 5",
                "Closed",
                null,
                "de:Alpines Museum (München)",
                "Q520455",
                "Alpines Museum in München, 2013.jpg"
        );
        SeeListing beerAndOktoberfestMuseum = new SeeListing(
                "Beer and Oktoberfest Museum",
                null,
                null,
                "+49 89 242 31607",
                "https://www.bier-und-oktoberfestmuseum.de/",
                "History of the festival, in a townhouse of 1340. The \"Himmelsleiter\" is its characteristic early staircase, a straight flight with no corners.",
                "Sterneckerstraße 2",
                "Tu-Sa 13:00-18:00",
                null,
                null,
                null,
                null
        );
        SeeListing valentinKarlstadt = new SeeListing(
                "Valentin-Karlstadt Musäum",
                null,
                null,
                "+49 89 223 266",
                "https://www.valentin-musaeum.de/",
                "Karl Valentin (Valentin Ludwig Fey, 1882-1948) was the Charlie Chaplin of Weimar Germany, appearing alongside a padded-up Liesl Karlstadt (Elisabeth Wellano, 1892-1960). Their typical guise was a pair of factory hands raging against their boss, and this quirky museum celebrates their style, which influenced many such as Brecht. The humour is mostly non-verbal but it will help if your German is good.",
                "Tal 50",
                "Th-Tu 11:00-18:00",
                null,
                null,
                null,
                null
        );

        SeeListing michaelskirche = new SeeListing(
                "Michaelskirche",
                48.1389,
                11.5705,
                "+49 89 231 7060",
                null,
                "Grand Renaissance church built for the Jesuits 1583-1599 by Duke Wilhelm V, as a bastion of the Counter-Reformation against those awful Lutherans. The Duke nearly busted the state coffers in buying up the land and funding a construction that townsfolk reckoned was too big to stand. And down fell the tower: \"I know, I'll build it bigger\". This version survived until bombed in wartime and was restored in the 1950s. The facade and interior continue the triumphal theme, for example the statue of Archangel Michael slaying a demon who might be Protestant. The church crypt holds many tombs of the Wittelsbach dynasty, including \"mad king\" Ludwig II.",
                "Neuhauser Straße 6",
                "M-Sa 08:00-19:00, Su 07:00-22:00",
                "Free",
                "St Michael's Church (Neuhauser Straße, Munich)",
                "Q265745",
                "Michaelskirche Muenchen-full.jpg"
        );

        SeeListing stadtmuseum = new SeeListing(
                "Münchner Stadtmuseum",
                48.1351,
                11.5730,
                "+49 89 233 22370",
                "https://www.muenchner-stadtmuseum.de/",
                "Extensive museum, occupying most of a city block; absorbing but signage is only in German. The oldest part of the building is an armory of 1500, the newest is the former Uhlfelder department store (all bombed in wartime, but the collections had been taken to safety), and it's growing still. The permanent exhibitions are \"Typically Munich!\" (how the city acquired its identity), Migration, National Socialism (the movement was launched here), Puppets and Fairground, and Music. \"Collections\" (whose highlights may figure in exhibitions) include photography, applied arts, graphics, textiles and music. Temporary exhibitions are generally on site for a year or so. There's also a film museum and theatre with daily screenings. Check ahead online for gallery closures, and search the online catalogue.",
                "St Jakobs Platz 1",
                "Tu-Su 10:00-18:00",
                "Adult €7, conc €3.50, under 18s free",
                "Munich Stadtmuseum",
                "Q452685",
                "Muenchener Stadtmuseum-1.jpg"
        );
        SeeListing jewishMuseum = new SeeListing(
                "Jewish Museum",
                null,
                null,
                "+49 89 233 96096",
                "http://www.juedisches-museum-muenchen.de",
                "Small museum of Jewish life in the city - and death. It adjoins the Ohel Jakov synagogue and the Jewish community centre.",
                "St Jakobs Platz 16",
                "Tu-Su 10:00-18:00",
                "Adult €6, conc €3, under 18 free",
                "Jewish Museum Munich",
                "Q320903",
                "München Jüdisches Zentrum und Jüdisches Museum.jpg"
        );
        SeeListing muca = new SeeListing(
                "Museum of Urban and Contemporary Art",
                48.1366,
                11.5700,
                "+49 89 215 524310",
                "https://www.muca.eu/",
                "Street and urban art is ephemeral but the idea of MUCA, opened in 2016, is to bring it into the artistic mainstream. There's a permanent collection and rotating exhibitions.",
                "Hotterstraße 12",
                "W-Su 10:00-18:00",
                "Adult €9, under 12 free",
                "Museum of Urban and Contemporary Art",
                null,
                null
        );
        SeeListing asamkirche = new SeeListing(
                "Asamkirche",
                48.1350,
                11.5694,
                null,
                "https://alterpeter.de/nebenkirchen/#asamkirche",
                "This Late Baroque confection was built from 1733 to 1746 as a private chapel by Egid Quirin Asam - he could see the altar from his house next door. His brother, Cosmas Damian Asam, did the frescoes. The church is dedicated to St John Nepomuk (1345-93), a Bohemian monk cast into the river Vltava in Prague at the behest of King Wenceslaus IV. Legend has it that Nepomuk refused to divulge the queen's confessional secrets, but his real crime was to back the wrong candidate for abbot. The church is wonderful by candlelight, especially at the Christmas Eve service, replete with Bavarian singers in the choir stall.",
                "Sendlinger Straße 32",
                "Su-Th 09:00-18:00, F 13:00-18:00",
                null,
                "Asam Church, Munich",
                "Q152973",
                "Asam-kirche 1.jpg"
        );

        List<SeeListing> expected = Arrays.asList(
                altesRathaus,
                neuesRathaus,
                peterskirche,
                frauenkirche,
                viktualienmarkt,
                theatinerkirche,
                kunsthalle,
                residenz,
                hofgartenMarker,
                deutschesTheatermuseum,
                stateChancellery,
                hausDerKunst,
                monopteros,
                bayerischesNationalmuseum,
                schackGallery,
                museumFuenfKontinente,
                kunstfoyerVkb,
                praterIsland,
                alpineMuseum,
                beerAndOktoberfestMuseum,
                isartor,
                valentinKarlstadt,
                maximiliansplatzMarker,
                briennerStrasse,
                michaelskirche,
                stadtmuseum,
                jewishMuseum,
                muca,
                asamkirche
        );
        List<SeeListing> parse = TemplateMatcher.parse(munich);
        assertEquals(expected.size(), parse.size());
        assertEquals(expected, parse);
    }

    @Test
    public void testEdinburghWestAll(
            @GivenTextResource("Edinburgh_west_all.txt") String edinburghWest) {
        SeeListing lochrinBasin = new SeeListing(
                "Lochrin Basin",
                55.9427,
                -3.2081,
                null,
                null,
                "is the east terminus of the Union Canal. When completed in 1822 it continued a little further to Tolcross at the edge of Old Town. It fell into disuse and was closed in 1965, but re-opened in 2001 and is fully navigable, with a firm towpath for strolling or cycling. The canal courses west to Slateford, vaulting over the Water of Leith by an aqueduct, and out to Kingsknowe, Wester Hailes and eventually to Falkirk, where a wheel hoists boats into the Forth and Clyde Canal.",
                null,
                null,
                null,
                null,
                null,
                null
        );
        SeeListing edinburghPrintmakers = new SeeListing(
                "Edinburgh Printmakers",
                55.9422474,
                -3.213065437,
                "+44 131 557-2479",
                "https://edinburghprintmakers.co.uk/",
                "Creative centre for printmaking & visual arts, Two free galleries & shop. Vegan Cafe.",
                "Castle Mills, 1 Dundee Street",
                "Su & M closed; Tu 10:00-17:30; W & Th 10:00-21:00; F & Sa 10:17:30",
                "Free for galleries",
                null,
                "Q105320053",
                null
        );
        SeeListing edinburghZoo = new SeeListing(
                "Edinburgh Zoo",
                55.9424,
                -3.2693,
                "+44 131 334-9171",
                "https://www.edinburghzoo.org.uk/",
                "The pandas have returned to China but there's still plenty here. Penguins include gentoo, rockhopper and king, most famously Major General Sir Nils Olav III, Baron of the Bouvet Islands and mascot of the Norwegian King's Guard. Budongo Trail leads through the troupe of 15 chimpanzees. The only koalas in the UK are here, and there are some beautiful tropical birds. The zoo is on the slopes of Corstorphine Hill and some paths are steep but all attractions are wheelchair accessible. Check the website for cheaper online offers, and temporary closures eg in harsh weather or avian flu outbreaks.",
                "134 Corstorphine Road EH12 6TS",
                "Daily Apr-Sep 9AM-6PM, Oct & Mar 9AM-5PM, Nov-Feb 9AM-4PM",
                "Adult £26.70, child £16.80, conc £24.25, parking £3",
                "Edinburgh Zoo",
                "Q1284778",
                null
        );
        SeeListing corstorphineOldParish = new SeeListing(
                "Corstorphine Old Parish Church",
                55.9414,
                -3.2819,
                null,
                "https://www.corstorphineoldparish.org.uk",
                "The present building is a collegiate church of 1429, becoming a parish church in 1646, and re-modeled in 1828.",
                "Kirk Loan EH12 7ST",
                null,
                null,
                "Corstorphine Old Parish Church",
                "Q17570258",
                null
        );
        SeeListing lauristonCastle = new SeeListing(
                "Lauriston Castle",
                55.9712,
                -3.2784,
                "+44 131 336-2060",
                "https://www.edinburghmuseums.org.uk/venue/lauriston-castle",
                "This 1590s tower house was extended in the 1820s and housed a series of prosperous families. It passed into council ownership in 1926 and the interior has been preserved in its last occupants' plush Edwardian style. Visit by pre-booked guided tour. There is free entry to the gardens (daily 8AM-4:30PM), where the centrepiece is the Japanese garden. The castle is also used as an event space.",
                "2 Cramond Road South EH4 6AD",
                "Sa Su 11AM-3PM",
                "Tour adult £8, conc £6",
                "Lauriston Castle",
                "Q2969689",
                null
        );
        SeeListing cramond = new SeeListing(
                "Cramond",
                55.9803,
                -3.3003,
                null,
                null,
                "is an attractive little village at the outlet of the River Almond, popular with amateur painters. It may be older than Edinburgh itself, as there are Mesolithic remains from 8500 BC, and the Romans built a fort later called Caer Amon, fort on the Almond, hence Cramond. There's a little sandy beach behind the breakwater but the Forth this far up is muddy. Walks lead upstream to a waterfall and main road, out to Cramond Island at low tide, and east along the esplanade to Silverknowes. The ferry no longer rows across the Almond, so you have to divert upstream to join the coast trail west towards South Queensferry. Cramond has a pub and tearooms. The free parking lot is small and fills up on fine afternoons, or take Bus 41.",
                null,
                null,
                null,
                null,
                null,
                null
        );
        SeeListing silverknowes = new SeeListing(
                "Silverknowes",
                55.9797,
                -3.2795,
                null,
                null,
                "a mile east along the esplanade has lots of free parking, so it can be a better starting point for visiting Cramond. The esplanade continues east for two miles but becomes industrial towards Granton Harbour.",
                null,
                null,
                null,
                null,
                null,
                null
        );
        SeeListing balerno = new SeeListing(
                "Balerno",
                55.8849,
                -3.3404,
                null,
                null,
                ": see Edinburgh/South for this village at the head of the Water of Leith Walkway, Malleny Gardens, Currie rugby club, and access to the Pentland Hills.",
                null,
                null,
                null,
                null,
                null,
                null
        );
        SeeListing murrayfieldStadium = new SeeListing(
                "Murrayfield Stadium",
                55.9422,
                -3.2406,
                "+44 131 378-1600",
                "https://scottishrugby.org/",
                "The main stadium has a capacity of 67,000 and hosts all Scottish rugby internationals. The premier tournament is the Six Nations held each February-March: in odd-numbered years, Scotland hosts Wales, Ireland and Italy, and in even-numbered years the visitors are England and France. These games almost always sell out, and the city is mobbed when they're on; you've more chance of tickets and accommodation for other internationals which take place each November, or, if it's a Rugby World Cup year, warm-up friendly games in the summer. Stadium tours are available on non-match days.",
                "Roseburn Street EH12 5PJ",
                null,
                null,
                "Murrayfield Stadium",
                "Q1024926",
                null
        );
        SeeListing murrayfieldIceRink = new SeeListing(
                "Murrayfield Ice Rink",
                null,
                null,
                "+44 131 337-6933",
                "https://www.murrayfieldicerinkltd.co.uk/",
                "Popular ice rink with leisure skating sessions, and ice-discos on Friday and Saturday nights. It's the home rink of the Edinburgh Capitals ice hockey team, who play Sept-March in the Scottish National League, the UK second tier. (Their rivals Murrayfield Racers folded in 2022.) The curling club also play here on a separate rink.",
                "13 Riversdale Crescent EH12 5XN",
                "M 2-7PM, W Th 2-9PM, F Sa 10AM-9PM, Su 11:30AM-7PM",
                null,
                null,
                null,
                null
        );
        SeeListing cineworld = new SeeListing(
                "Cineworld",
                55.9413,
                -3.2179,
                "+44 330 333 4444",
                "https://www.cineworld.co.uk/cinemas/edinburgh",
                "Mainly mainstream cinema with some art-house & foreign cinema. IMAX screen.",
                "130 Dundee St EH11 1AF",
                null,
                null,
                null,
                null,
                null
        );
        SeeListing corstorphineHill = new SeeListing(
                "Corstorphine Hill",
                55.9508,
                -3.2726,
                null,
                "https://www.corstorphinehill.org.uk/",
                "rises above the zoo to 528 ft / 161 m. It's a wooded park with multiple access points, popular with walkers, joggers and winter tobogganists, and its south is serenaded by exotic bellows from the beasts in the zoo. The tower near the top is dedicated to Sir Walter Scott, as if his Gothic Rocket on Princes Street wasn't enough. 200 yards west of the tower by Clermiston Rd is a walled garden. Views are somewhat restricted by the woods but here and there are vistas of the castle and sea.",
                null,
                null,
                null,
                null,
                null,
                null
        );
        SeeListing royalHighlandShowground = new SeeListing(
                "Royal Highland Showground",
                55.9411,
                -3.369,
                null,
                "https://www.royalhighlandcentre.co.uk/",
                "An exhibition ground with several halls and large grounds. The main event here is the Royal Highland Show for 4 days in late June. Other events are held throughout the year, some of these are public, others are trade only.",
                "Ingliston EH28 8NB",
                null,
                null,
                "Royal Highland Showground",
                "Q7374277",
                null
        );
        SeeListing edinburghClimbingArena = new SeeListing(
                "Edinburgh International Climbing Arena",
                55.9235,
                -3.3979,
                "+44 131 458-2100",
                "https://www.edinburghleisure.co.uk/venues/edinburgh-international-climbing-arena",
                "Huge indoor and outdoor climbing centre in a disused quarry, developed from 1995 but taken over and completed by Edinburgh City Council in 2007.",
                "South Platt Hill, Ratho EH28 8AA",
                null,
                null,
                "Edinburgh International Climbing Arena",
                null,
                null
        );
        SeeListing hearts = new SeeListing(
                "Heart of Midlothian Football Club",
                55.939139,
                -3.230944,
                "+44 333 043 1874",
                "https://www.heartsfc.co.uk",
                "Hearts aka \"the Jambos\" and \"the Gorgie Boys\" play football (soccer) in the Scottish Premiership, the game's top tier. Their home ground Tynecastle Park (capacity 20,000, all-seater) is a mile west of Haymarket station in the Gorgie area of the city; take any bus up Gorgie Road or a tram to Murrayfield.",
                "Gorgie Road EH11 2NL",
                null,
                "~£30 adult vs Celtic Hibernian and Rangers, slightly cheaper for other games",
                "Tynecastle Stadium",
                "Q1326446",
                "Tynecastle Stadium 2007.jpg"
        );
        SeeListing spartans = new SeeListing(
                "The Spartans FC",
                55.971528,
                -3.233111,
                "+44 333 043 1874",
                null,
                "The Spartans play football in Scottish League Two, the game's fourth tier. Their ground, Ainslie Park (Vanloq Community Stadium) holds 3,612, of which 534 are seated. The ground is located in the Pilton area, northwest of the city centre.",
                "94 Pilton Dr, Edinburgh EH5 2HF",
                null,
                null,
                "The Spartans F.C.",
                null,
                null
        );
        SeeListing edinburghFilmhouse = new SeeListing(
                "Edinburgh Filmhouse",
                55.946439,
                -3.206139,
                "+44 131 380-5280",
                "https://filmhouse.org.uk/",
                "A four-screen Edinburgh institution showing largely arthouse and some mainstream films. Additionally houses a 100-seat cafe-bar.",
                "88 Lothian Rd, Edinburgh EH3 9BZ",
                "10AM-late",
                "£12 adult, £10.50 concession, wheelchair users/access tickets £5, carers free",
                "Edinburgh Filmhouse",
                null,
                "Exterior of Filmhouse Cafe Bar, Lothian Road, Edinburgh (5474231345).jpg"
        );
        
        List<SeeListing> expected = Arrays.asList(
                lochrinBasin,
                edinburghPrintmakers,
                edinburghZoo,
                corstorphineOldParish,
                lauristonCastle,
                cramond,
                silverknowes,
                balerno,
                murrayfieldStadium,
                murrayfieldIceRink,
                cineworld,
                corstorphineHill,
                royalHighlandShowground,
                edinburghClimbingArena,
                hearts,
                spartans,
                edinburghFilmhouse
        );
        List<SeeListing> parse = TemplateMatcher.parse(edinburghWest);

        assertEquals(expected.size(), parse.size());
        assertEquals(expected, parse);
    }
}