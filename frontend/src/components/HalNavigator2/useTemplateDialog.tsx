import {type ReactElement, type ReactNode, useCallback, useState} from 'react';
import {createPortal} from 'react-dom';
import type {HalFormsTemplate, HalResourceLinks} from '../../api';
import {toHref} from '../../api/hateoas.ts';
import {useHalPageData} from '../../hooks/useHalPageData.ts';
import {getDialogTitleLabel, getTemplateLabel} from '../../localization';
import {Modal} from '../UI';
import {HalFormDisplay} from './HalFormDisplay.tsx';
import type {HalFormFieldFactory} from './halforms';

export interface TemplateResource {
    _templates?: unknown;
    _links?: unknown;
}

export const defaultDialogTitle = (name: string, template: HalFormsTemplate): string | undefined =>
    getDialogTitleLabel(name) ?? getTemplateLabel(name) ?? template.title;

export interface TemplateDialogOptions {
    resource?: TemplateResource | null;
    title?: string | ((template: HalFormsTemplate, name: string) => string);
    size?: 'sm' | 'md' | 'lg' | 'xl' | '2xl' | '4xl';
    note?: ReactNode;
    submitLabel?: string;
    successMessage?: string;
    onSubmitSuccess?: (responseData?: unknown) => void;
    onSubmitError?: (error: unknown) => boolean | void;
    navigateOnSuccess?: boolean;
    fieldsFactory?: HalFormFieldFactory;
    initialData?: Record<string, unknown>;
    postprocessPayload?: (payload: Record<string, unknown>, template: HalFormsTemplate) => Record<string, unknown>;
}

export type TemplateDialogOverrides = Pick<
    TemplateDialogOptions,
    'title' | 'note' | 'onSubmitSuccess' | 'successMessage' | 'navigateOnSuccess'
>;

export interface TemplateDialog {
    available: boolean;
    availableOn: (resource: TemplateResource | null | undefined, name?: string) => boolean;
    open: (resource?: TemplateResource | null, name?: string, overrides?: TemplateDialogOverrides) => void;
    dialog: ReactElement | null;
}

interface OpenDialog {
    resource: TemplateResource;
    name: string;
    overrides?: TemplateDialogOverrides;
}

const templateOf = (resource: TemplateResource | null | undefined, name: string | undefined): HalFormsTemplate | undefined =>
    name ? (resource?._templates as Record<string, HalFormsTemplate> | undefined)?.[name] : undefined;

const selfHref = (resource: TemplateResource): string | undefined => {
    const self = (resource._links as {self?: HalResourceLinks} | undefined)?.self;
    return self ? toHref(self) : undefined;
};

export function useTemplateDialog(name?: string, options: TemplateDialogOptions = {}): TemplateDialog {
    const {route: {pathname}} = useHalPageData();
    const [opened, setOpened] = useState<OpenDialog | null>(null);
    const {resource: defaultResource} = options;

    const availableOn = useCallback(
        (resource: TemplateResource | null | undefined, templateName?: string) =>
            templateOf(resource, templateName ?? name) !== undefined,
        [name],
    );

    const open = useCallback((resource?: TemplateResource | null, templateName?: string, overrides?: TemplateDialogOverrides) => {
        const target = resource ?? defaultResource;
        const targetName = templateName ?? name;
        if (target && targetName && templateOf(target, targetName)) {
            setOpened({resource: target, name: targetName, overrides});
        }
    }, [defaultResource, name]);

    const close = useCallback(() => setOpened(null), []);

    const defaultTemplate = templateOf(defaultResource, name);

    const openedTemplate = opened ? templateOf(opened.resource, opened.name) : undefined;
    let dialog: ReactElement | null = null;
    if (opened && openedTemplate) {
        const effective = {...options, ...opened.overrides};
        const title = typeof effective.title === 'function'
            ? effective.title(openedTemplate, opened.name)
            : effective.title ?? defaultDialogTitle(opened.name, openedTemplate);

        dialog = createPortal(
            <Modal isOpen onClose={close} title={title} size={effective.size ?? 'md'} context={effective.note}>
                <HalFormDisplay
                    template={openedTemplate}
                    templateName={opened.name}
                    resourceData={(effective.initialData ?? opened.resource) as Record<string, unknown>}
                    pathname={pathname}
                    resourceUrl={selfHref(opened.resource)}
                    onClose={close}
                    onSubmitSuccess={(data) => {
                        effective.onSubmitSuccess?.(data);
                        close();
                    }}
                    onSubmitError={effective.onSubmitError}
                    successMessage={effective.successMessage}
                    submitButtonLabel={effective.submitLabel}
                    navigateOnSuccess={effective.navigateOnSuccess}
                    fieldsFactory={effective.fieldsFactory}
                    postprocessPayload={effective.postprocessPayload}
                />
            </Modal>,
            document.body,
        );
    }

    return {available: defaultTemplate !== undefined, availableOn, open, dialog};
}
