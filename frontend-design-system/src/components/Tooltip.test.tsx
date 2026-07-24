import {render, screen} from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import {Tooltip} from './Tooltip';

describe('Tooltip', () => {
    it('renders the trigger children', () => {
        render(<Tooltip content="tip text"><button>trigger</button></Tooltip>);
        expect(screen.getByText('trigger')).toBeInTheDocument();
    });

    it('does not render the tooltip until hovered', () => {
        render(<Tooltip content="tip text"><span>trigger</span></Tooltip>);
        expect(screen.queryByRole('tooltip')).not.toBeInTheDocument();
    });

    it('shows the tooltip content on hover', async () => {
        const user = userEvent.setup();
        render(<Tooltip content="tip text"><span>trigger</span></Tooltip>);
        await user.hover(screen.getByText('trigger'));
        expect(screen.getByRole('tooltip')).toHaveTextContent('tip text');
    });

    it('renders multi-line content on hover', async () => {
        const user = userEvent.setup();
        render(<Tooltip content={'line 1\nline 2'}><span>trigger</span></Tooltip>);
        await user.hover(screen.getByText('trigger'));
        expect(screen.getByRole('tooltip')).toHaveTextContent('line 1 line 2');
    });
});
