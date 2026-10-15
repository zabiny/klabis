import {render, screen} from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import {DisplayContactText} from './DisplayContactText';

const hint = 'kontakt zákonného zástupce (Jana Nováková)';
const guardian = {value: 'rodic@example.com', fromGuardian: true, guardianName: 'Jana Nováková'};

describe('DisplayContactText', () => {
    it('shows dash without value', () => {
        render(<DisplayContactText contact={{}}/>);
        expect(screen.getByText('—')).toBeInTheDocument();
    });

    it('shows no icon for own contact', () => {
        render(<DisplayContactText contact={{value: 'a@b.cz', fromGuardian: false}}/>);
        expect(screen.getByText('a@b.cz')).toBeInTheDocument();
        expect(screen.queryByRole('button')).not.toBeInTheDocument();
    });

    it('shows info button with label but hides popup by default', () => {
        render(<DisplayContactText contact={guardian}/>);
        expect(screen.getByRole('button', {name: hint})).toBeInTheDocument();
        expect(screen.queryByRole('tooltip')).not.toBeInTheDocument();
        expect(screen.queryByText(hint)).not.toBeInTheDocument();
    });

    it('shows popup on hover', async () => {
        const user = userEvent.setup();
        render(<DisplayContactText contact={guardian}/>);
        await user.hover(screen.getByRole('button'));
        expect(screen.getByRole('tooltip')).toHaveTextContent(hint);
        await user.unhover(screen.getByRole('button'));
        expect(screen.queryByRole('tooltip')).not.toBeInTheDocument();
    });

    it('shows popup on keyboard focus and hides on blur', async () => {
        const user = userEvent.setup();
        render(<DisplayContactText contact={guardian}/>);
        await user.tab();
        expect(screen.getByRole('tooltip')).toHaveTextContent(hint);
        await user.tab();
        expect(screen.queryByRole('tooltip')).not.toBeInTheDocument();
    });

    it('toggles popup on click and closes on outside tap', async () => {
        const user = userEvent.setup();
        render(<div><DisplayContactText contact={guardian}/><p>outside</p></div>);
        await user.click(screen.getByRole('button'));
        expect(screen.getByRole('tooltip')).toBeInTheDocument();
        await user.click(screen.getByText('outside'));
        expect(screen.queryByRole('tooltip')).not.toBeInTheDocument();
    });

    it('closes popup on Escape', async () => {
        const user = userEvent.setup();
        render(<DisplayContactText contact={guardian}/>);
        await user.click(screen.getByRole('button'));
        await user.keyboard('{Escape}');
        expect(screen.queryByRole('tooltip')).not.toBeInTheDocument();
    });

    it('does not propagate click to parent (row click)', async () => {
        const user = userEvent.setup();
        const onRow = vi.fn();
        render(<div onClick={onRow}><DisplayContactText contact={guardian}/></div>);
        await user.click(screen.getByRole('button'));
        expect(onRow).not.toHaveBeenCalled();
    });
});
