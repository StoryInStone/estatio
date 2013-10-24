/*
 *
 *  Copyright 2012-2013 Eurocommercial Properties NV
 *
 *
 *  Licensed under the Apache License, Version 2.0 (the
 *  "License"); you may not use this file except in compliance
 *  with the License.  You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing,
 *  software distributed under the License is distributed on an
 *  "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 *  KIND, either express or implied.  See the License for the
 *  specific language governing permissions and limitations
 *  under the License.
 */
package org.estatio.dom.party;

import java.util.Collection;
import java.util.SortedSet;
import java.util.TreeSet;

import javax.jdo.annotations.DiscriminatorStrategy;
import javax.jdo.annotations.IdGeneratorStrategy;
import javax.jdo.annotations.IdentityType;
import javax.jdo.annotations.VersionStrategy;

import com.google.common.base.Predicates;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;

import org.apache.isis.applib.annotation.AutoComplete;
import org.apache.isis.applib.annotation.Bookmarkable;
import org.apache.isis.applib.annotation.Disabled;
import org.apache.isis.applib.annotation.Hidden;
import org.apache.isis.applib.annotation.Named;
import org.apache.isis.applib.annotation.NotPersisted;
import org.apache.isis.applib.annotation.RegEx;
import org.apache.isis.applib.annotation.Render;
import org.apache.isis.applib.annotation.Render.Type;
import org.apache.isis.applib.annotation.Title;

import org.estatio.dom.EstatioMutableObject;
import org.estatio.dom.WithNameComparable;
import org.estatio.dom.WithReferenceUnique;
import org.estatio.dom.agreement.AgreementRole;
import org.estatio.dom.agreement.AgreementType;
import org.estatio.dom.agreement.AgreementTypes;
import org.estatio.dom.communicationchannel.CommunicationChannelOwner;
import org.estatio.dom.financial.FinancialConstants;
import org.estatio.dom.lease.LeaseConstants;

@javax.jdo.annotations.PersistenceCapable(identityType = IdentityType.DATASTORE)
@javax.jdo.annotations.DatastoreIdentity(
        strategy = IdGeneratorStrategy.NATIVE,
        column = "id")
@javax.jdo.annotations.Version(
        strategy = VersionStrategy.VERSION_NUMBER,
        column = "version")
@javax.jdo.annotations.Discriminator(
        strategy = DiscriminatorStrategy.CLASS_NAME,
        column = "discriminator")
@javax.jdo.annotations.Uniques({
        @javax.jdo.annotations.Unique(
                name = "Party_reference_UNQ", members = "reference")
})
@javax.jdo.annotations.Indices({
        // to cover the 'findByReferenceOrName' query
        // both in this superclass and the subclasses
        @javax.jdo.annotations.Index(
                name = "Party_reference_name_IDX", members = { "reference", "name" })
})
@javax.jdo.annotations.Queries({
        @javax.jdo.annotations.Query(
                name = "findByReferenceOrName", language = "JDOQL",
                value = "SELECT "
                        + "FROM org.estatio.dom.party.Party "
                        + "WHERE reference.matches(:referenceOrName) "
                        + "   || name.matches(:referenceOrName)"),
        @javax.jdo.annotations.Query(
                name = "findByReference", language = "JDOQL",
                value = "SELECT "
                        + "FROM org.estatio.dom.party.Party "
                        + "WHERE reference == :reference") })
@AutoComplete(repository = Parties.class, action = "autoComplete")
@Bookmarkable
public abstract class Party
        extends EstatioMutableObject<Party>
        implements WithNameComparable<Party>, WithReferenceUnique, CommunicationChannelOwner {

    public Party() {
        super("name");
    }


    // //////////////////////////////////////

    private String reference;

    @javax.jdo.annotations.Column(allowsNull = "false")
    @RegEx(validation = "[-/_A-Z0-9]+", caseSensitive = false)
    @Disabled
    public String getReference() {
        return reference;
    }

    public void setReference(final String reference) {
        this.reference = reference;
    }

    // //////////////////////////////////////

    private String name;

    @javax.jdo.annotations.Column(allowsNull = "false")
    @Title
    public String getName() {
        return name;
    }

    public void setName(final String name) {
        this.name = name;
    }

    /**
     * Provided so that subclasses can override and disable.
     */
    public String disableName() {
        return null;
    }

    // //////////////////////////////////////

    @javax.jdo.annotations.Persistent(mappedBy = "party")
    private SortedSet<AgreementRole> agreements = new TreeSet<AgreementRole>();

    @Hidden
    public SortedSet<AgreementRole> getAgreements() {
        return agreements;
    }

    public void setAgreements(final SortedSet<AgreementRole> agreements) {
        this.agreements = agreements;
    }

    // //////////////////////////////////////

    @NotPersisted
    @Render(Type.EAGERLY)
    public Collection<AgreementRole> getLeases() {
        return listCurrentAgreementsOfType(LeaseConstants.AT_LEASE);
    }

    @NotPersisted
    @Render(Type.EAGERLY)
    public Collection<AgreementRole> getBankMandates() {
        return listCurrentAgreementsOfType(FinancialConstants.AT_MANDATE);
    }

    private Collection<AgreementRole> listCurrentAgreementsOfType(final String art) {
        final AgreementType agreementType = agreementTypes.find(art);
        return Lists.newArrayList(
                Iterables.filter(getAgreements(),
                        Predicates.and(
                                AgreementRole.whetherAgreementTypeIs(agreementType),
                                AgreementRole.whetherCurrentIs(true))));
    }

    @Named("List All")
    public Collection<AgreementRole> listAllLeases() {
        return listAgreementsOfType(LeaseConstants.AT_LEASE);
    }

    @Named("List All")
    public Collection<AgreementRole> listAllMandates() {
        return listAgreementsOfType(FinancialConstants.AT_MANDATE);
    }

    private Collection<AgreementRole> listAgreementsOfType(final String art) {
        final AgreementType agreementType = agreementTypes.find(art);
        return Lists.newArrayList(
                Iterables.filter(getAgreements(),
                        AgreementRole.whetherAgreementTypeIs(agreementType)));
    }

    // //////////////////////////////////////

    // TODO: EST-86. is a bidir mapping required?
    // @javax.jdo.annotations.Persistent(mappedBy = "party")
    private SortedSet<PartyRegistration> registrations = new TreeSet<PartyRegistration>();

    @Hidden
    public SortedSet<PartyRegistration> getRegistrations() {
        return registrations;
    }

    public void setRegistrations(final SortedSet<PartyRegistration> registrations) {
        this.registrations = registrations;
    }

    @Hidden
    public Party newRegistration() {
        return this;
    }

    // //////////////////////////////////////

    private AgreementTypes agreementTypes;

    public final void injectAgreementTypes(final AgreementTypes agreementTypes) {
        this.agreementTypes = agreementTypes;
    }

}
