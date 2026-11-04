package com.spacedrep.application.view;

import java.util.List;

/** Fila do dia + quanto ainda falta depois desta pagina. */
public record DueQueue(List<DueCard> cards, long remaining) {
}
