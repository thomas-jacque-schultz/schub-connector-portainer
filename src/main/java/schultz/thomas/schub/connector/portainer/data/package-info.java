/**
 * Couche données — <strong>volontairement vide</strong>.
 *
 * <p>Ce connecteur ne persiste rien : Portainer est la source de vérité, et la sonde d'état
 * n'entretient qu'un cache en mémoire ({@code StackStateService}). Le paquet existe pour que le
 * découpage {@code api} / {@code business} / {@code data} se lise de la même façon dans tous les
 * services — son absence se lirait comme un oubli, pas comme une intention.</p>
 *
 * <p>Si un jour ce connecteur persiste quelque chose, c'est ici, et lui seul y touche : la règle
 * est que {@code data} ne connaît personne, {@code business} connaît {@code data}, et
 * {@code api} connaît {@code business}.</p>
 */
package schultz.thomas.schub.connector.portainer.data;
