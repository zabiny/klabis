import '@testing-library/jest-dom';
import {render, screen} from '@testing-library/react';
import {SelectField} from './SelectField.tsx';

describe('SelectField', () => {
    it('associates the label with the select when no id is passed', () => {
        render(<SelectField label="Typ pravidla" options={[{value: 'a', label: 'A'}]} onChange={() => {}}/>);
        expect(screen.getByLabelText('Typ pravidla').tagName).toBe('SELECT');
    });
});
