/**
 * (C) Copyright IBM Corp. 2019
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package com.ibm.watsonhealth.fhir.model.type;

import java.util.Collection;

public class ContributorType extends Code {
    /**
     * Author
     */
    public static final ContributorType AUTHOR = ContributorType.of(ValueSet.AUTHOR);

    /**
     * Editor
     */
    public static final ContributorType EDITOR = ContributorType.of(ValueSet.EDITOR);

    /**
     * Reviewer
     */
    public static final ContributorType REVIEWER = ContributorType.of(ValueSet.REVIEWER);

    /**
     * Endorser
     */
    public static final ContributorType ENDORSER = ContributorType.of(ValueSet.ENDORSER);

    private ContributorType(Builder builder) {
        super(builder);
    }

    public static ContributorType of(java.lang.String value) {
        return ContributorType.builder().value(value).build();
    }

    public static ContributorType of(ValueSet value) {
        return ContributorType.builder().value(value).build();
    }

    public static String string(java.lang.String value) {
        return ContributorType.builder().value(value).build();
    }

    public static Code code(java.lang.String value) {
        return ContributorType.builder().value(value).build();
    }

    public static Builder builder() {
        return new Builder();
    }

    public Builder toBuilder() {
        Builder builder = new Builder();
        builder.id = id;
        builder.extension.addAll(extension);
        builder.value = value;
        return builder;
    }

    public static class Builder extends Code.Builder {
        private Builder() {
            super();
        }

        @Override
        public Builder id(java.lang.String id) {
            return (Builder) super.id(id);
        }

        @Override
        public Builder extension(Extension... extension) {
            return (Builder) super.extension(extension);
        }

        @Override
        public Builder extension(Collection<Extension> extension) {
            return (Builder) super.extension(extension);
        }

        @Override
        public Builder value(java.lang.String value) {
            return (Builder) super.value(ValueSet.from(value).value());
        }

        public Builder value(ValueSet value) {
            return (Builder) super.value(value.value());
        }

        @Override
        public ContributorType build() {
            return new ContributorType(this);
        }
    }

    public enum ValueSet {
        /**
         * Author
         */
        AUTHOR("author"),

        /**
         * Editor
         */
        EDITOR("editor"),

        /**
         * Reviewer
         */
        REVIEWER("reviewer"),

        /**
         * Endorser
         */
        ENDORSER("endorser");

        private final java.lang.String value;

        ValueSet(java.lang.String value) {
            this.value = value;
        }

        public java.lang.String value() {
            return value;
        }

        public static ValueSet from(java.lang.String value) {
            for (ValueSet c : ValueSet.values()) {
                if (c.value.equals(value)) {
                    return c;
                }
            }
            throw new IllegalArgumentException(value);
        }
    }
}