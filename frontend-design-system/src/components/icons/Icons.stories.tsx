import type {Meta, StoryObj} from '@storybook/react-vite'
import {ComputerDesktopIcon} from './ComputerDesktopIcon'
import {DeleteIcon} from './DeleteIcon'
import {EditIcon} from './EditIcon'
import {LogoutIcon} from './LogoutIcon'
import {MoonIcon} from './MoonIcon'
import {NewspaperIcon} from './NewspaperIcon'
import {SunIcon} from './SunIcon'
import {SyncIcon} from './SyncIcon'

const allIcons = [
    {name: 'ComputerDesktopIcon', Icon: ComputerDesktopIcon},
    {name: 'DeleteIcon', Icon: DeleteIcon},
    {name: 'EditIcon', Icon: EditIcon},
    {name: 'LogoutIcon', Icon: LogoutIcon},
    {name: 'MoonIcon', Icon: MoonIcon},
    {name: 'NewspaperIcon', Icon: NewspaperIcon},
    {name: 'SunIcon', Icon: SunIcon},
    {name: 'SyncIcon', Icon: SyncIcon},
]

const meta = {
    title: 'Components/Icons',
    tags: ['autodocs'],
} satisfies Meta

export default meta
type Story = StoryObj<typeof meta>

export const Gallery: Story = {
    render: () => (
        <div className="grid grid-cols-4 gap-6">
            {allIcons.map(({name, Icon}) => (
                <div
                    key={name}
                    className="flex flex-col items-center gap-2 border border-border rounded-md p-4 bg-surface-raised"
                >
                    <Icon size={24} className="text-text-primary"/>
                    <span className="text-xs text-text-secondary text-center">{name}</span>
                </div>
            ))}
        </div>
    ),
}

export const Sizes: Story = {
    render: () => (
        <div className="flex items-end gap-6">
            <div className="flex flex-col items-center gap-2">
                <EditIcon size={16} className="text-text-primary"/>
                <span className="text-xs text-text-secondary">16</span>
            </div>
            <div className="flex flex-col items-center gap-2">
                <EditIcon size={24} className="text-text-primary"/>
                <span className="text-xs text-text-secondary">24</span>
            </div>
            <div className="flex flex-col items-center gap-2">
                <EditIcon size={32} className="text-text-primary"/>
                <span className="text-xs text-text-secondary">32</span>
            </div>
            <div className="flex flex-col items-center gap-2">
                <EditIcon size={48} className="text-text-primary"/>
                <span className="text-xs text-text-secondary">48</span>
            </div>
        </div>
    ),
}

export const Colors: Story = {
    render: () => (
        <div className="flex items-center gap-6">
            <DeleteIcon size={28} className="text-error"/>
            <EditIcon size={28} className="text-primary"/>
            <SyncIcon size={28} className="text-success"/>
            <LogoutIcon size={28} className="text-text-secondary"/>
        </div>
    ),
}

export const ThemeToggleIcons: Story = {
    render: () => (
        <div className="flex items-center gap-6 text-text-primary">
            <div className="flex flex-col items-center gap-2">
                <SunIcon size={24}/>
                <span className="text-xs text-text-secondary">Světlý režim</span>
            </div>
            <div className="flex flex-col items-center gap-2">
                <MoonIcon size={24}/>
                <span className="text-xs text-text-secondary">Tmavý režim</span>
            </div>
            <div className="flex flex-col items-center gap-2">
                <ComputerDesktopIcon size={24}/>
                <span className="text-xs text-text-secondary">Podle systému</span>
            </div>
        </div>
    ),
}
