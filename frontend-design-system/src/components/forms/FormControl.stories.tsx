import type {Meta, StoryObj} from '@storybook/react-vite'
import {FormControl} from './FormControl'
import {TextField} from './TextField'
import {SelectField} from './SelectField'
import {SwitchField} from './SwitchField'

const meta = {
    title: 'Components/Forms/FormControl',
    component: FormControl,
    tags: ['autodocs'],
    args: {
        children: null,
    },
} satisfies Meta<typeof FormControl>

export default meta
type Story = StoryObj<typeof meta>

export const Default: Story = {
    render: () => (
        <FormControl>
            <TextField label="Jméno" placeholder="Jan Novák"/>
            <TextField label="E-mail" type="email" placeholder="jan.novak@klabis.cz"/>
        </FormControl>
    ),
}

export const MixedFields: Story = {
    render: () => (
        <div className="w-96">
            <FormControl>
                <TextField label="Jméno" placeholder="Jan Novák"/>
                <SelectField
                    label="Klub"
                    placeholder="Vyberte klub"
                    options={[
                        {value: 'zbm', label: 'SK Žabovřesky'},
                        {value: 'lokomotiva', label: 'Lokomotiva Brno'},
                    ]}
                />
                <SwitchField name="active" label="Aktivní členství" checked/>
            </FormControl>
        </div>
    ),
}
