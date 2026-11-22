import {type ReactElement} from "react";
import {useNavigate} from "react-router-dom";
import {useHalPageData} from "../../hooks/useHalPageData.ts";
import {asLinkArray} from "../../api/hateoas.ts";
import {toRoutePath} from "../../api/apiPath.ts";
import {SelectField} from "../../components/UI/forms/SelectField.tsx";
import {labels} from "../../localization";

export const AccountSwitcher = (): ReactElement | null => {
    const {resourceData} = useHalPageData();
    const navigate = useNavigate();

    const options = asLinkArray(resourceData?._links?.viewableAccount);
    if (options.length < 2) {
        return null;
    }

    const selfLink = asLinkArray(resourceData?._links?.self)[0];
    const currentPath = selfLink ? toRoutePath(selfLink.href) : undefined;

    return (
        <div data-testid="account-switcher" className="sm:max-w-xs">
            <SelectField
                label={labels.finance.accountSwitcher}
                value={currentPath ?? ""}
                onChange={(e) => navigate(e.target.value)}
                options={options.map((option) => ({value: toRoutePath(option.href), label: option.title ?? ""}))}
            />
        </div>
    );
};
