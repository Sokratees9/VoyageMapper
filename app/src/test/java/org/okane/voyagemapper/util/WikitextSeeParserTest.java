package org.okane.voyagemapper.util;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;
import org.okane.voyagemapper.model.SeeListing;

import java.util.Arrays;
import java.util.List;

import io.hosuaby.inject.resources.junit.jupiter.GivenTextResource;
import io.hosuaby.inject.resources.junit.jupiter.TestWithResources;

@TestWithResources
public class WikitextSeeParserTest {

    @Test
    public void parsePortarlington(
            @GivenTextResource("Portarlington_full_article.txt") String portarlington) {

        SeeListing marketSquare = new SeeListing("Market Square", 53.162, -7.190, null, null,
                "is the town centre, with 18th- and 19th-century low-rise in the streets around. The Huguenot tradition really only lives on in the name French Church Street, referring to St Paul's (Church of Ireland). Services were held in French in its predecessor into the 1820s, but the present church was built over the site in 1851. St Michael's is the Roman Catholic church, west on Patrick St.",
                null, null, null, null, null);
        SeeListing carrickWood = new SeeListing("Carrick Wood", 53.144, -7.188, null, null,
                "is on a small hill south side of town. Stroll up from the car park to \"The Spire\", an 18th-century folly probably made from the ruin of a windmill. The town waterworks are adjacent.",
                null, null, null, null, null);
        SeeListing leaCastle = new SeeListing("Lea Castle", 53.156, -7.147, null, null,
                "is a ruin along R420 four km east of town. It was built in fits and starts over the 13th century, replacing a timber and earthwork structure, and in its pomp was a mighty four-towered donjon or keep with an inner and outer moat and bawn wall. But it was wrecked on multiple occasions, and was already in a sorry state when Cromwell's troops came by in 1650, and wrecked it some more just to keep their hands in. One last tenant lived here until 1737. The ruin is overgrown and unsafe, so it's closed to the public, and work is under way to try to stabilise what remains.",
                null, null, null, null, null);
        SeeListing emoCourt = new SeeListing("Emo Court", 53.107, -7.197, "+353 57 862 6573", "https://emocourt.ie/",
                "Grand neo-classical mansion, designed by Gandon in 1790 but construction took another 70 years. Then at last the Earls of Portarlington could enjoy it and entertain in glittering style, but these were the final halcyon days of the Protestant ascendancy. Arms Quarterly 1st & 4th, Barry nebuly of six Argent and Gules, over all a Bend engrailed Azure... (and so on, at some length) somehow didn't enthrall an independent Republic of Ireland. The family left, and the house lay empty until 1930, then was a Jesuit college until 1969. Not wishing to sully their pious gaze with Grecian nymphs, the Jesuits chucked the statues in the lake. The estate was bought by Major Cholmeley Harrison, who restored the house, fished out the statues and opened the gardens to the public. House and gardens are now curated by the Office of Public Works, but the house is closed for restoration until summer 2024.",
                "Emo R32 C44V", "Grounds daily 8:30AM-4:30PM", null, null, null);

        List<SeeListing> things = WikitextSeeParser.parse(portarlington);

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
                "CasaBatllo 0055.JPG"
        );
        SeeListing laPedrera = new SeeListing(
                "La Pedrera",
                41.395278,
                2.161667,
                "+34 932 142 576",
                "https://www.lapedrera.com/en",
                "One of Gaudi's masterpieces, there is not one straight edge on the exterior and the structure is one of Gaudi’s most eccentric and enticing architectural creations. Tours of the interior and the incredible roof structures are available. Great views of the tall buildings (especially La Sagrada Familia) from the roof. Hosts a large exposition of Gaudi works, covering ''Sagrada Familia'' and ''Casa Batlio'', not only ''La Pedrera'' itself. The exposition is a good place to start your exploration of Gaudi; it reveals many hidden details for the art novice.",
                "Passeig de Gràcia 92",
                "09:00-20:30",
                "from €29, children 0-6 free, discounts for older children, students & 65+. €2 additional charge if buying from ticket office",
                "Casa Milà",
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
                "Casa Amatller - Barcelona 2014.jpg"
        );
        SeeListing casaLleoMorera = new SeeListing(
                "Casa Lleó-Morera",
                41.391244,
                2.165528,
                "+34 936 762 733",
                "http://www.casalleomorera.com/",
                "By [[w:Casa Lleó Morera|Lluís Domènech i Montaner]].",
                "Passeig de Gràcia 35",
                "Tu-Su 10:00-13:30, 15:00-19:00 (only guided tours roughly every 1/2 hr, full tour in English at 11:00)",
                "Adult €15 (sold online or at [http://ajuntament.barcelona.cat/lavirreina/en/ Palau de la Virreina] (La Rambla, 99)",
                "Casa Lleó Morera",
                "CasaLleoMorera-Full.jpg"
        );
        SeeListing fundacioAntoniTapies = new SeeListing(
                "Fundació Antoni Tàpies",
                41.39167,
                2.16361,
                "",
                "https://fundaciotapies.org/en/",
                "Created in 1984 by the artist Antoni Tàpies to promote the study and knowledge of modern and contemporary art.",
                "C/d'Aragó, 255",
                "",
                "",
                "Fundació Antoni Tàpies",
                "Domènech.i.Montaner.Editorial.Montaner.i.Simón.1.Barcelona.JPG"
        );
        SeeListing hotelMajestic = new SeeListing(
                "Hotel Majestic",
                41.393406,
                2.163947,
                "+34 934 881 717",
                "http://www.hotelmajestic.es/",
                "Passeig de Gràcia 68. Enjoy city views from the rooftop. Just enter the elevator right from the lobby, and head up to the top floor.",
                "",
                "",
                "",
                "Hotel Majestic (Barcelona)",
                "Hotel Majestic Barcelona.JPG"
        );
        SeeListing placaDeCatalunya = new SeeListing(
                "Plaça de Catalunya",
                41.386667,
                2.17,
                "",
                "",
                "Connecting many of the major streets in the city, the Plaça is a central location and a hub of activity. It is a favourite meeting spot for locals and a transit hub for the tourist buses. There are many fountains, statues and gardens around the plaza, and a number of benches to just sit and take it all in.",
                "at the end of Passeig de Gràcia and the top of La Rambla",
                "",
                "",
                "Plaça de Catalunya",
                "050529 Barcelona 109.jpg"
        );
        SeeListing universitat = new SeeListing(
                "Universitat",
                41.386667,
                2.163889,
                "",
                "http://www.ub.cat",
                "The university has many interesting buildings, libraries and courtyards.",
                "",
                "",
                "",
                "University of Barcelona",
                "PlacaUniversitat.jpg"
        );

        List<SeeListing> expected = Arrays.asList(
                casaBatllo,
                laPedrera,
                casaAmatller,
                casaLleoMorera,
                fundacioAntoniTapies,
                hotelMajestic,
                placaDeCatalunya,
                universitat,
                sagradaFamilia
        );

        List<SeeListing> parse = WikitextSeeParser.parse(eixample);

        assertEquals(expected.size(), parse.size());
        assertEquals(expected, parse);
    }

    @Test
    public void parsesSingleLineMarkerSee() {
        String wikitext = "* {{marker | name=Lochrin Basin | type=see | lat=55.9427 | long=-3.2081 }} is the east terminus.";

        List<SeeListing> out = WikitextSeeParser.parse(wikitext); // whatever your class is called
        assertEquals(1, out.size());

        SeeListing s = out.get(0);
        assertEquals("Lochrin Basin", s.name());
        assertEquals(55.9427, s.lat(), 1e-6);
        assertEquals(-3.2081, s.lon(), 1e-6);
        // tail content comes from the prose after }}
        assertTrue(s.content().contains("east terminus"));
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

        List<SeeListing> out = WikitextSeeParser.parse(wikitext);
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

        List<SeeListing> out = WikitextSeeParser.parse(wikitext);
        // You only want type=see/do → must be skipped
        assertTrue(out.isEmpty());
    }

    // Test Munich old town, Portarlington and Edinburgh West data
}