import type {Meta, StoryObj} from '@storybook/react-vite'
import {useState} from 'react'
import {Toast, ToastContainer, type ToastMessage} from './Toast'
import {Button} from './Button'

const baseToast = (overrides: Partial<ToastMessage> = {}): ToastMessage => ({
    id: '1',
    message: 'Člen Jan Novák byl úspěšně uložen.',
    type: 'success',
    ...overrides,
})

const meta = {
    title: 'Components/Toast',
    component: Toast,
    tags: ['autodocs'],
    args: {
        toast: baseToast(),
        onClose: () => {},
    },
} satisfies Meta<typeof Toast>

export default meta
type Story = StoryObj<typeof meta>

export const Default: Story = {
    render: () => {
        const [toast, setToast] = useState<ToastMessage | null>(baseToast())
        if (!toast) {
            return <Button onClick={() => setToast(baseToast())}>Zobrazit znovu</Button>
        }
        return <Toast toast={toast} onClose={() => setToast(null)}/>
    },
}

export const Success: Story = {
    render: () => <Toast toast={baseToast({type: 'success', message: 'Platba byla úspěšně zaevidována.'})} onClose={() => {}}/>,
}

export const ErrorToast: Story = {
    name: 'Error',
    render: () => <Toast toast={baseToast({type: 'error', message: 'Uložení člena selhalo. Zkuste to prosím znovu.'})} onClose={() => {}}/>,
}

export const Warning: Story = {
    render: () => <Toast toast={baseToast({type: 'warning', message: 'Registrační číslo se blíží expiraci.'})} onClose={() => {}}/>,
}

export const Info: Story = {
    render: () => <Toast toast={baseToast({type: 'info', message: 'Nová verze systému bude nasazena v neděli v 22:00.'})} onClose={() => {}}/>,
}

export const WithAction: Story = {
    render: () => (
        <Toast
            toast={baseToast({
                type: 'warning',
                message: 'Formulář má neuložené změny.',
                action: {label: 'Uložit nyní', onClick: () => alert('Uloženo')},
            })}
            onClose={() => {}}
        />
    ),
}

export const ContainerWithMultipleToasts: Story = {
    render: () => {
        const [toasts, setToasts] = useState<ToastMessage[]>([
            baseToast({id: '1', type: 'success', message: 'Člen byl uložen.'}),
            baseToast({id: '2', type: 'error', message: 'Platbu se nepodařilo zpracovat.'}),
            baseToast({id: '3', type: 'info', message: 'Export dat byl dokončen.'}),
        ])
        return (
            <div className="relative h-64 w-full">
                <ToastContainer
                    toasts={toasts}
                    onClose={(id) => setToasts((prev) => prev.filter((t) => t.id !== id))}
                    position="top-right"
                />
            </div>
        )
    },
}
