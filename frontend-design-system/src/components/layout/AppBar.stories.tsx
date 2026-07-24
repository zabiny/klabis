import type {Meta, StoryObj} from '@storybook/react-vite'
import {AppBar, Toolbar} from './AppBar'
import {Button} from '../Button'
import {LogoutIcon} from '../icons/LogoutIcon'

const meta = {
    title: 'Components/Layout/AppBar',
    component: AppBar,
    tags: ['autodocs'],
    argTypes: {
        position: {control: 'select', options: ['static', 'relative', 'fixed', 'sticky']},
        elevation: {control: 'select', options: ['none', 'sm', 'md', 'lg']},
        color: {control: 'select', options: ['default', 'primary']},
    },
    args: {
        position: 'relative',
        elevation: 'md',
        color: 'default',
        children: null,
    },
} satisfies Meta<typeof AppBar>

export default meta
type Story = StoryObj<typeof meta>

export const Default: Story = {
    render: (args) => (
        <AppBar {...args}>
            <Toolbar>
                <span className="font-semibold text-text-primary">SK Žabovřesky</span>
                <Button variant="ghost" size="sm" endIcon={<LogoutIcon size={16}/>}>
                    Odhlásit
                </Button>
            </Toolbar>
        </AppBar>
    ),
}

export const Primary: Story = {
    args: {color: 'primary'},
    render: (args) => (
        <AppBar {...args}>
            <Toolbar>
                <span className="font-semibold">Klabis</span>
                <nav className="flex gap-4 text-sm">
                    <span>Členové</span>
                    <span>Akce</span>
                    <span>Závody</span>
                </nav>
            </Toolbar>
        </AppBar>
    ),
}

export const WithNavigationAndUserMenu: Story = {
    render: (args) => (
        <AppBar {...args}>
            <Toolbar>
                <div className="flex items-center gap-6">
                    <span className="font-semibold text-text-primary">SK Žabovřesky</span>
                    <nav className="flex gap-4 text-sm text-text-secondary">
                        <span className="text-primary font-medium">Členové</span>
                        <span>Akce</span>
                        <span>Tréninkové skupiny</span>
                        <span>Závody</span>
                    </nav>
                </div>
                <div className="flex items-center gap-3">
                    <span className="text-sm text-text-secondary">Jan Novák (ZBM1234)</span>
                    <Button variant="ghost" size="sm" endIcon={<LogoutIcon size={16}/>}>
                        Odhlásit
                    </Button>
                </div>
            </Toolbar>
        </AppBar>
    ),
}

export const DenseToolbar: Story = {
    render: (args) => (
        <AppBar {...args}>
            <Toolbar variant="dense">
                <span className="font-semibold text-text-primary text-sm">SK Žabovřesky</span>
                <Button variant="ghost" size="sm">Odhlásit</Button>
            </Toolbar>
        </AppBar>
    ),
}

export const ToolbarStandalone: Story = {
    render: () => (
        <div className="bg-surface-raised border border-border rounded-md">
            <Toolbar>
                <span className="font-semibold text-text-primary">Detail člena</span>
                <div className="flex gap-2">
                    <Button variant="secondary" size="sm">Zrušit</Button>
                    <Button variant="primary" size="sm">Uložit</Button>
                </div>
            </Toolbar>
        </div>
    ),
}
