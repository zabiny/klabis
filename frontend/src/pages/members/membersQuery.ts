import type {MembersFilterValue} from '../../components/members/MembersFilterBar.tsx';

const MIN_SEARCH_LENGTH = 2;

export function toQuery(values: MembersFilterValue): Record<string, string> {
    const query: Record<string, string> = {status: values.status};
    if (values.q.length >= MIN_SEARCH_LENGTH) query.q = values.q;
    if (values.incomplete) query.incomplete = 'true';
    return query;
}
