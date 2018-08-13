package com.meryt.demographics.response;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import lombok.Getter;
import lombok.NonNull;

import com.meryt.demographics.domain.family.LeastCommonAncestorRelationship;
import com.meryt.demographics.domain.person.Person;
import com.meryt.demographics.service.PersonService;

@Getter
class LeastCommonAncestorResponseTree extends PersonReference {

    private List<LeastCommonAncestorResponseTree> children = new ArrayList<>();

    /**
     * @param person the person of interest
     * @param otherPerson the related person
     * @param lcaRelationship the least common ancestor relationship between these two people
     * @param personService the person service, used to load the tree of related persons (is not saved in the object
     *                      but only used in the constructor)
     */
    LeastCommonAncestorResponseTree(@NonNull Person person,
                                           @NonNull Person otherPerson,
                                           @NonNull LeastCommonAncestorRelationship lcaRelationship,
                                           @NonNull PersonService personService) {

        super(personService.load(lcaRelationship.getLeastCommonAncestor()));

        String[] personOneIds = lcaRelationship.getSubject1Via() == null
                ? new String[0]
                : lcaRelationship.getSubject1Via().split(",");
        String[] personTwoIds = lcaRelationship.getSubject2Via() == null
                ? new String[0]
                : lcaRelationship.getSubject2Via().split(",");

        children.add(new LeastCommonAncestorResponseTree(personOneIds, person, personService));
        children.add(new LeastCommonAncestorResponseTree(personTwoIds, otherPerson, personService));
    }

    private LeastCommonAncestorResponseTree(@NonNull String[] path,
                                            @NonNull Person terminalPerson,
                                            @NonNull PersonService personService) {
        super(path.length == 0 ? terminalPerson : personService.load(Long.valueOf(path[0])));
        if (path.length == 1) {
            children.add(new LeastCommonAncestorResponseTree(new String[0], terminalPerson, personService));
        } else if (path.length > 1) {
            children.add(new LeastCommonAncestorResponseTree(Arrays.copyOfRange(path, 1, path.length), terminalPerson,
                    personService));
        }
    }

}
