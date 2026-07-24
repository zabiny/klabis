import type {Meta, StoryObj} from '@storybook/react-vite'
import {useState} from 'react'
import {Modal} from './Modal'
import {Button} from './Button'

const meta = {
    title: 'Components/Modal',
    component: Modal,
    tags: ['autodocs'],
    argTypes: {
        title: {control: 'text'},
        size: {
            control: 'select',
            options: ['sm', 'md', 'lg', 'xl', '2xl', '4xl'],
        },
        closeButton: {control: 'boolean'},
        closeOnBackdropClick: {control: 'boolean'},
    },
    args: {
        isOpen: true,
        onClose: () => {},
        title: 'Smazat člena',
        children: 'Opravdu chcete odstranit člena Jan Novák (ZBM4821) z klubu?',
    },
} satisfies Meta<typeof Modal>

export default meta
type Story = StoryObj<typeof meta>

const OpenModalTemplate = (args: Partial<React.ComponentProps<typeof Modal>>) => {
    const [isOpen, setIsOpen] = useState(true)
    return (
        <div>
            <Button onClick={() => setIsOpen(true)}>Otevřít dialog</Button>
            <Modal
                isOpen={isOpen}
                onClose={() => setIsOpen(false)}
                title="Smazat člena"
                {...args}
            >
                <p className="text-sm text-text-secondary">
                    Opravdu chcete odstranit člena Jan Novák (ZBM4821) z klubu? Tuto akci nelze vrátit zpět.
                </p>
            </Modal>
        </div>
    )
}

export const Default: Story = {
    render: (args) => <OpenModalTemplate {...args} />,
}

export const WithFooterActions: Story = {
    render: () => {
        const [isOpen, setIsOpen] = useState(true)
        return (
            <div>
                <Button onClick={() => setIsOpen(true)}>Otevřít dialog</Button>
                <Modal
                    isOpen={isOpen}
                    onClose={() => setIsOpen(false)}
                    title="Smazat člena"
                    footer={
                        <>
                            <Button variant="secondary" onClick={() => setIsOpen(false)}>Zrušit</Button>
                            <Button variant="danger" onClick={() => setIsOpen(false)}>Smazat</Button>
                        </>
                    }
                >
                    <p className="text-sm text-text-secondary">
                        Opravdu chcete odstranit člena Jan Novák (ZBM4821) z klubu?
                    </p>
                </Modal>
            </div>
        )
    },
}

export const WithoutCloseButton: Story = {
    render: (args) => <OpenModalTemplate {...args} closeButton={false}/>,
}

export const Sizes: Story = {
    render: () => {
        const [openSize, setOpenSize] = useState<'sm' | 'md' | 'lg' | 'xl' | '2xl' | '4xl' | null>(null)
        const sizes = ['sm', 'md', 'lg', 'xl', '2xl', '4xl'] as const
        return (
            <div className="flex gap-3">
                {sizes.map((size) => (
                    <Button key={size} size="sm" onClick={() => setOpenSize(size)}>{size}</Button>
                ))}
                {openSize && (
                    <Modal
                        isOpen
                        onClose={() => setOpenSize(null)}
                        title={`Velikost: ${openSize}`}
                        size={openSize}
                    >
                        <p className="text-sm text-text-secondary">Obsah dialogu ve velikosti {openSize}.</p>
                    </Modal>
                )}
            </div>
        )
    },
}

export const WithoutTitle: Story = {
    render: (args) => <OpenModalTemplate {...args} title={undefined}/>,
}
