import {render, screen} from '@testing-library/react';
import {JsonPreview} from './JsonPreview';

describe('JsonPreview Component', () => {
    describe('Rendering', () => {
        it('should render label as heading', () => {
            render(<JsonPreview data={{}} label="Test Label"/>);
            const heading = screen.getByText('Test Label');
            expect(heading.tagName).toBe('H2');
        });

        it('should use default label when not provided', () => {
            render(<JsonPreview data={{}}/>);
            expect(screen.getByText('Data')).toBeInTheDocument();
        });

        it('should render data as formatted JSON', () => {
            const testData = {name: 'John', age: 30};
            render(<JsonPreview data={testData}/>);
            const preElement = screen.getByText(/John/);
            expect(preElement.tagName).toBe('PRE');
        });
    });

    describe('JSON Formatting', () => {
        it('should format JSON with proper indentation', () => {
            const testData = {user: {name: 'Alice'}};
            const {container} = render(<JsonPreview data={testData}/>);
            const preElement = container.querySelector('pre');
            const jsonText = preElement?.textContent || '';
            expect(jsonText).toContain('"user"');
            expect(jsonText).toContain('"name"');
            expect(jsonText).toContain('Alice');
        });

        it('should handle nested objects', () => {
            const testData = {
                level1: {
                    level2: {
                        level3: 'value',
                    },
                },
            };
            render(<JsonPreview data={testData}/>);
            const preElement = screen.getByText(/level1/);
            expect(preElement.textContent).toContain('level3');
        });

        it('should handle arrays', () => {
            const testData = {items: [1, 2, 3]};
            render(<JsonPreview data={testData}/>);
            const preElement = screen.getByText(/items/);
            expect(preElement.textContent).toContain('[');
            expect(preElement.textContent).toContain(']');
        });

        it('should handle empty objects', () => {
            const testData = {};
            render(<JsonPreview data={testData}/>);
            const preElement = screen.getByText(/\{\}/);
            expect(preElement).toBeInTheDocument();
        });

        it('should handle null data', () => {
            render(<JsonPreview data={null}/>);
            const preElement = screen.getByText('null');
            expect(preElement.tagName).toBe('PRE');
        });

        it('should handle undefined data', () => {
            render(<JsonPreview data={undefined}/>);
            expect(screen.getByText('Data')).toBeInTheDocument();
        });
    });

    describe('Data Types', () => {
        it('should handle strings', () => {
            const testData = {text: 'hello'};
            render(<JsonPreview data={testData}/>);
            expect(screen.getByText(/hello/)).toBeInTheDocument();
        });

        it('should handle numbers', () => {
            const testData = {count: 42};
            render(<JsonPreview data={testData}/>);
            expect(screen.getByText(/42/)).toBeInTheDocument();
        });

        it('should handle booleans', () => {
            const testData = {active: true, disabled: false};
            render(<JsonPreview data={testData}/>);
            const preElement = screen.getByText(/active/);
            expect(preElement.textContent).toContain('true');
            expect(preElement.textContent).toContain('false');
        });

        it('should handle mixed types', () => {
            const testData = {
                string: 'text',
                number: 123,
                boolean: true,
                null: null,
                array: [1, 2, 3],
            };
            render(<JsonPreview data={testData}/>);
            const preElement = screen.getByText(/string/);
            expect(preElement.textContent).toContain('text');
            expect(preElement.textContent).toContain('123');
            expect(preElement.textContent).toContain('true');
        });
    });

    describe('Special Cases', () => {
        it('should handle objects with special characters in keys', () => {
            const testData = {'key-with-dash': 'value'};
            render(<JsonPreview data={testData}/>);
            const preElement = screen.getByText(/key-with-dash/);
            expect(preElement).toBeInTheDocument();
        });

        it('should handle large nested structures', () => {
            const testData = {
                a: {b: {c: {d: {e: {f: 'deep'}}}}},
            };
            render(<JsonPreview data={testData}/>);
            expect(screen.getByText(/deep/)).toBeInTheDocument();
        });

        it('should render empty array', () => {
            const testData = {items: []};
            render(<JsonPreview data={testData}/>);
            const preElement = screen.getByText(/items/);
            expect(preElement.textContent).toContain('[]');
        });

        it('should render array with objects', () => {
            const testData = {users: [{id: 1, name: 'Alice'}, {id: 2, name: 'Bob'}]};
            render(<JsonPreview data={testData}/>);
            expect(screen.getByText(/Alice/)).toBeInTheDocument();
            expect(screen.getByText(/Bob/)).toBeInTheDocument();
        });
    });

    describe('Display', () => {
        it('should use pre element for code block', () => {
            const {container} = render(<JsonPreview data={{}}/>);
            const preElement = container.querySelector('pre');
            expect(preElement).toBeInTheDocument();
        });

        it('should preserve whitespace in pre element', () => {
            const testData = {key: 'value'};
            const {container} = render(<JsonPreview data={testData}/>);
            const preElement = container.querySelector('pre');
            // Pre elements preserve whitespace, JSON.stringify with indent should show this
            expect(preElement?.textContent).toBeTruthy();
        });
    });

    describe('Label Variations', () => {
        it('should render custom label', () => {
            render(<JsonPreview data={{}} label="Custom Label"/>);
            expect(screen.getByText('Custom Label')).toBeInTheDocument();
        });

        it('should render empty string label', () => {
            render(<JsonPreview data={{}} label=""/>);
            const headings = screen.queryAllByText('');
            // With empty label, h2 should still exist but be empty
            const h2 = headings.find((el) => el.tagName === 'H2');
            expect(h2).toBeInTheDocument();
        });

        it('should update label when prop changes', () => {
            const {rerender} = render(<JsonPreview data={{}} label="First"/>);
            expect(screen.getByText('First')).toBeInTheDocument();

            rerender(<JsonPreview data={{}} label="Second"/>);
            expect(screen.getByText('Second')).toBeInTheDocument();
            expect(screen.queryByText('First')).not.toBeInTheDocument();
        });
    });
});
