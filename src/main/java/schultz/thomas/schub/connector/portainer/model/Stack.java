package schultz.thomas.schub.connector.portainer.model;

import java.time.Instant;

/**
 * Une stack telle que Portainer la rapportait à un instant donné.
 *
 * <p>Ce connecteur ne sait pas ce qu'est un serveur de jeu : il reflète Portainer, rien de plus.
 * C'est l'appelant qui trie ce qui l'intéresse.</p>
 *
 * @param observedAt date de la lecture qui a produit cette valeur. <strong>C'est le champ le plus
 *                   important du contrat.</strong> Les réponses viennent d'un cache : sans cette
 *                   date, un appelant qui démarre une stack puis relit aussitôt conclurait
 *                   qu'elle est toujours éteinte. Une valeur trop vieille doit être traitée
 *                   comme une absence de réponse, pas comme un état.
 */
public record Stack(
        Integer id,
        String name,
        Integer endpointId,
        boolean running,
        Instant observedAt
) {
}
