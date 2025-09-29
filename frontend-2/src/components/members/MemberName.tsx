import React, {ReactNode} from 'react';
import {useGetMember} from "../../api/membersApi";

interface MemberNameProps {
    memberId: number,
    loadingValue?: ReactNode
}

function MemberName({memberId, loadingValue = "Loading..."}: MemberNameProps) {
    const {data: member, isLoading} = useGetMember(memberId);

    if (isLoading) {
        return <>${loadingValue}</>;
    }

    if (!member) {
        console.warn(`No member data returned from API for member Id ${memberId}`)
        return <span>Neznamý člověk</span>
    }

    return (
        <span>{member.firstName} {member.lastName}</span>
    );
}

export default MemberName;