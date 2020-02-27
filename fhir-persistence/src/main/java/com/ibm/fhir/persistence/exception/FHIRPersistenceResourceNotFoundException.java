/*
 * (C) Copyright IBM Corp. 2016, 2020
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package com.ibm.fhir.persistence.exception;

import com.ibm.fhir.model.type.code.IssueType;
import com.ibm.fhir.model.util.FHIRUtil;

public class FHIRPersistenceResourceNotFoundException extends FHIRPersistenceException {
    private static final long serialVersionUID = 1L;

    /**
     * Create an exception with a single OperationOutcome.Issue of type NOT_FOUND,
     * both with the passed message
     */
    public FHIRPersistenceResourceNotFoundException(String message) {
        super(message);
        withIssue(FHIRUtil.buildOperationOutcomeIssue(message, IssueType.NOT_FOUND));
    }

    /**
     * Create an exception with the passed message and cause and 
     * a single OperationOutcome.Issue with the passed message and an IssueType of NOT_FOUND
     */
    public FHIRPersistenceResourceNotFoundException(String message, Throwable cause) {
        super(message, cause);
        withIssue(FHIRUtil.buildOperationOutcomeIssue(message, IssueType.NOT_FOUND));
    }
}
