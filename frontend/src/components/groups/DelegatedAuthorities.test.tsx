import '@testing-library/jest-dom';
import {render, screen} from '@testing-library/react';
import {DelegatedAuthoritiesNotice, DelegatedAuthoritiesReadOnly} from './DelegatedAuthorities.tsx';

describe('DelegatedAuthoritiesNotice', () => {
    it('lists the delegated permissions for the owners view and says future owners are included', () => {
        render(<DelegatedAuthoritiesNotice authorities={['MEMBERS:EDIT_PROFILE']} audience="owners"/>);

        expect(screen.getByText('Vlastníci skupiny (i budoucí) mohou: Úprava údajů člena')).toBeInTheDocument();
    });

    it('states that owners gain nothing when no permission is delegated', () => {
        render(<DelegatedAuthoritiesNotice authorities={[]} audience="owners"/>);

        expect(screen.getByText('Vlastníci skupiny nad členy nezískávají žádná oprávnění.')).toBeInTheDocument();
    });

    it('tells the invitee that current and future owners get the permission by accepting', () => {
        render(<DelegatedAuthoritiesNotice authorities={['MEMBERS:EDIT_PROFILE']} audience="invitee"/>);

        expect(screen.getByText(
            'Přijetím pozvánky získají vlastníci skupiny (současní i budoucí) nad vámi oprávnění: Úprava údajů člena'
        )).toBeInTheDocument();
    });

    it('tells the invitee that owners gain nothing when no permission is delegated', () => {
        render(<DelegatedAuthoritiesNotice authorities={[]} audience="invitee"/>);

        expect(screen.getByText('Přijetím pozvánky nezískají vlastníci skupiny nad vámi žádná oprávnění.')).toBeInTheDocument();
    });

    it('falls back to the raw authority code when it has no label', () => {
        render(<DelegatedAuthoritiesNotice authorities={['SOMETHING:NEW']} audience="owners"/>);

        expect(screen.getByText(/SOMETHING:NEW/)).toBeInTheDocument();
    });

    it('renders nothing when the list is absent', () => {
        const {container} = render(<DelegatedAuthoritiesNotice authorities={undefined} audience="owners"/>);

        expect(container).toBeEmptyDOMElement();
    });
});

describe('DelegatedAuthoritiesReadOnly', () => {
    it('shows the delegated permissions as non-editable text', () => {
        render(<DelegatedAuthoritiesReadOnly authorities={['MEMBERS:EDIT_PROFILE']}/>);

        expect(screen.getByText('Oprávnění vlastníků nad členy')).toBeInTheDocument();
        expect(screen.getByText('Úprava údajů člena')).toBeInTheDocument();
        expect(screen.queryByRole('checkbox')).not.toBeInTheDocument();
    });

    it('shows "Žádná" when nothing is delegated', () => {
        render(<DelegatedAuthoritiesReadOnly authorities={[]}/>);

        expect(screen.getByText('Žádná')).toBeInTheDocument();
    });
});
