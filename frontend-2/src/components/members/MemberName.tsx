import {type ReactNode} from 'react';
import {useKlabisApiQuery} from "../../api";

interface MemberNameProps {
    memberId: number,
    loadingValue?: ReactNode
}

function MemberName({memberId, loadingValue = "Loading..."}: MemberNameProps) {
    const {
        data: member,
        isLoading
    } = useKlabisApiQuery("get", "/members/{memberId}", {params: {path: {memberId: memberId}}});

    if (isLoading) {
        return <>${loadingValue}</>;
    }

    if (!member) {
        console.warn(`No member data returned from API for member Id ${memberId}`)
        return <span className="text-text-secondary">Neznamý člověk</span>
    }

    return (
        <span className="text-text-primary">{member.firstName} {member.lastName}</span>
    );
}

export default MemberName;