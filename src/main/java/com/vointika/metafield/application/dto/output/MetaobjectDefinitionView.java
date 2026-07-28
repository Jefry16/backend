package com.vointika.metafield.application.dto.output;

import com.vointika.metafield.domain.entity.MetaobjectDefinition;
import com.vointika.metafield.domain.entity.MetaobjectField;

import java.util.List;

/** The detail read: the definition plus its fields ordered by position. */
public record MetaobjectDefinitionView(
        MetaobjectDefinition definition,
        List<MetaobjectField> fields) {
}
