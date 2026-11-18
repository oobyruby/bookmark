package uws.ac.uk.bookmark;

import java.text.Normalizer;
import java.util.Locale;

public class StephenKingSynopses {

    // retrieve the custom synopsis for the five selected stephen king books
    public static String getSynopsis(String title, String authors) {

        if (title == null || authors == null) {
            return null;
        }

        // only override books written by stephen king
        if (!normalise(authors).contains("stephen king")) {
            return null;
        }

        // match the book title regardless of capitalisation or punctuation
        String bookTitle = normalise(title);

        switch (bookTitle) {

            case "the shining":
                return "Jack Torrance, a struggling writer and recovering alcoholic, accepts a position as the winter caretaker of the Overlook Hotel, an isolated luxury resort nestled deep within the Colorado Rocky Mountains. Hoping to rebuild his troubled relationship with his wife, Wendy, and their young son, Danny, Jack views the opportunity as a chance for a fresh start. However, as the harsh winter approaches and heavy snowfall cuts the family off from the outside world, the hotel's sinister history begins to reveal itself.\n\n"
                        + "Unknown to his parents, five-year-old Danny possesses an extraordinary psychic ability known as \"the shining\", allowing him to perceive disturbing visions, communicate telepathically and sense the supernatural forces inhabiting the hotel. As Danny's terrifying experiences intensify, Jack gradually falls under the influence of the Overlook's malevolent presence. His already fragile mental state deteriorates, blurring the boundaries between reality, paranoia and supernatural manipulation.\n\n"
                        + "With no means of escape and an increasingly dangerous father, Wendy and Danny must confront the terrifying forces that have taken hold of their isolated sanctuary. What began as a peaceful winter retreat becomes a desperate struggle for survival, as the Overlook Hotel reveals that some places never truly forget their past.";

            case "it":
                return "In the seemingly ordinary town of Derry, Maine, an ancient and malevolent entity lurks beneath the streets, awakening every twenty-seven years to feed upon the fears of its inhabitants. Often appearing as Pennywise the Dancing Clown, the creature preys primarily upon children, exploiting their deepest anxieties and transforming itself into whatever terrifies them most.\n\n"
                        + "During the summer of 1958, seven outcast children form an unlikely friendship, calling themselves the Losers' Club. Each has experienced bullying, loneliness or personal hardship, but together they discover a shared connection through their encounters with the mysterious evil haunting their hometown. As children begin disappearing, the group becomes determined to uncover the truth behind Derry's horrifying history and confront the creature responsible.\n\n"
                        + "Nearly three decades later, a new wave of violence forces the now-adult members of the Losers' Club to return to Derry. Having largely forgotten the terrifying events of their childhood, they must recover their buried memories and honour a promise made long ago.\n\n"
                        + "Moving between childhood and adulthood, It explores the enduring power of friendship, the psychological scars of growing up and the ways in which fear can shape an entire community. As the past and present collide, the Losers must face not only an ancient supernatural horror, but also the childhood traumas they believed they had left behind.";

            case "carrie":
                return "Carrie White is a shy, socially isolated teenage girl who has spent most of her life enduring relentless bullying at school and oppressive religious control at home. Raised by her fanatically devout mother, Margaret, Carrie has been taught to fear the outside world, suppress her individuality and regard ordinary aspects of growing up as sinful. Her sheltered upbringing leaves her vulnerable to the cruelty of her classmates, who repeatedly humiliate her for being different.\n\n"
                        + "Following a particularly traumatic incident in the school changing rooms, Carrie begins experiencing strange and inexplicable phenomena. Objects move without being touched, and powerful emotions seem to trigger disturbances around her. Gradually, she discovers that she possesses telekinetic abilities, allowing her to manipulate objects through the power of her mind.\n\n"
                        + "As Carrie struggles to understand her emerging powers, an unexpected invitation to the senior prom offers her a rare opportunity to experience acceptance and happiness. For the first time, she begins to imagine a life beyond her mother's suffocating control and the hostility of her peers.\n\n"
                        + "However, beneath the excitement of the approaching dance, resentment and cruelty continue to simmer. When a devastating act of humiliation pushes Carrie beyond her emotional limits, her extraordinary abilities threaten to transform years of suffering into an unimaginable catastrophe.";

            case "misery":
                return "Paul Sheldon is a successful novelist best known for his bestselling series of historical romance novels featuring the beloved fictional character Misery Chastain. Determined to pursue more serious literary work, Paul decides to end the popular series and move forward with a new direction in his writing career. However, his plans take a terrifying turn when he suffers a severe car accident during a snowstorm in rural Colorado.\n\n"
                        + "Paul regains consciousness in the isolated home of Annie Wilkes, a former nurse who claims to be his devoted number-one fan. Initially grateful for her assistance, Paul soon discovers that Annie's cheerful demeanour conceals a deeply unstable and dangerous personality. With both legs badly injured and no means of contacting the outside world, he becomes entirely dependent upon her for medication, food and basic survival.\n\n"
                        + "When Annie discovers that Paul has killed off her favourite fictional character in his latest novel, her admiration transforms into violent obsession. Refusing to accept Misery's fate, she demands that Paul write an entirely new book restoring the character to life.\n\n"
                        + "Trapped inside Annie's remote farmhouse, Paul must use his intelligence, creativity and determination to survive. As her behaviour becomes increasingly unpredictable, the boundaries between devoted fandom and terrifying captivity disappear, leaving Paul fighting to escape a nightmare more horrifying than anything he has ever written.";

            case "pet sematary":
                return "Dr Louis Creed relocates with his wife, Rachel, their two young children, Ellie and Gage, and their family cat, Church, to the quiet rural town of Ludlow, Maine. Hoping to enjoy a peaceful new beginning, Louis accepts a position at a nearby university and settles into their new home. However, the family's seemingly idyllic property conceals a disturbing secret within the dense woodland behind their house.\n\n"
                        + "Their elderly neighbour, Jud Crandall, introduces the family to a small burial ground maintained by generations of local children, marked by a weathered sign reading \"Pet Sematary\". Beyond this innocent resting place lies a much older and more sinister burial ground, hidden deep within the forest and surrounded by ancient legends.\n\n"
                        + "When tragedy strikes the Creed family, Louis learns that the mysterious ground possesses an unnatural power capable of returning the dead to the living world. Yet those who return are not always the same as those who were buried.\n\n"
                        + "Overwhelmed by grief and unable to accept the finality of death, Louis becomes increasingly consumed by the possibility of reversing an unimaginable loss. As desperation clouds his judgement, he begins to disregard the warnings surrounding the burial ground.\n\n"
                        + "Pet Sematary explores the devastating consequences of grief, the human desire to defy mortality and the terrifying possibility that some boundaries should never be crossed.";

            default:
                // return null so other books retain their original api synopsis
                return null;
        }
    }

    // normalise titles and authors for consistent matching
    private static String normalise(String value) {

        return Normalizer.normalize(
                        value,
                        Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "")
                .toLowerCase(Locale.ROOT)
                .replaceAll("[^a-z0-9]+", " ")
                .trim();
    }
}
