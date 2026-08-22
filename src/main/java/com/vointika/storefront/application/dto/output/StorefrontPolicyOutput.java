package com.vointika.storefront.application.dto.output;

import com.vointika.shared.port.StorefrontTourOperatorQuery.PolicyDetailView;

/** The globals plus the policy this route is about. */
public record StorefrontPolicyOutput(StorefrontGlobals globals, PolicyDetailView policy) {}
