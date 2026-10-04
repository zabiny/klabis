package com.klabis.common.security.fieldsecurity;

import tools.jackson.core.JsonGenerator;
import tools.jackson.databind.PropertyName;
import tools.jackson.databind.ser.BeanPropertyWriter;
import tools.jackson.databind.util.NameTransformer;
import com.klabis.common.authorization.AuthorizationEvaluator;
import com.klabis.common.users.HasAuthority;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.authorization.method.HandleAuthorizationDenied;
import tools.jackson.databind.SerializationContext;

import java.lang.reflect.Method;
import java.util.function.Supplier;

/**
 * Wraps a {@link BeanPropertyWriter} to evaluate security annotations ({@link PreAuthorize},
 * {@link HasAuthority}, or {@link OwnerVisible}) during serialization. When authorization is
 * denied the field is either masked or skipped entirely, depending on the
 * {@link HandleAuthorizationDenied} configuration resolved from the record component or class level.
 * <p>
 * The decision is made by {@link AuthorizationEvaluator#canReadField}, so it is the same one that guards the
 * request fields and the HAL-FORMS properties.
 * <p>
 * Overrides {@link #_new(PropertyName)} and {@link #unwrappingWriter(NameTransformer)} rather than
 * relying on the {@link BeanPropertyWriter} base implementations. Jackson calls these when a
 * property is renamed (via the public {@code rename(NameTransformer)}, which dispatches to
 * {@code _new} polymorphically) or unwrapped (e.g. {@code @JsonUnwrapped}); the base
 * implementations construct a plain {@code BeanPropertyWriter}, which would otherwise strip this
 * wrapper — and with it the security check — exactly like the bug this class already had to work
 * around for {@code JsonNullable} properties (see {@link FieldSecurityBeanSerializerModifier}).
 * Both return a new {@link SecuredBeanPropertyWriter} wrapping the equivalently transformed
 * delegate (produced via the delegate's own public {@code rename}/{@code unwrappingWriter}, since
 * {@code _new} itself is protected and not callable on an arbitrary {@code BeanPropertyWriter}
 * reference), so the wrapper survives regardless of what other writer transformations run against
 * it, in any order.
 * <p>
 * Before these overrides existed, {@code BeanPropertyWriter}'s base {@code _new} threw
 * {@code IllegalStateException} for any subclass that did not override it — so a Jackson-triggered
 * rename of a secured property (e.g. via {@code @JsonUnwrapped}) would have crashed outright rather
 * than quietly dropping the security check. These overrides close that latent crash risk as well.
 */
class SecuredBeanPropertyWriter extends BeanPropertyWriter {

    private final BeanPropertyWriter delegate;
    private final Method accessorMethod;
    private final HandleAuthorizationDenied deniedHandler;
    private final Supplier<AuthorizationEvaluator> evaluator;

    SecuredBeanPropertyWriter(
            BeanPropertyWriter delegate,
            Method accessorMethod,
            HandleAuthorizationDenied deniedHandler,
            Supplier<AuthorizationEvaluator> evaluator) {
        super(delegate);
        this.delegate = delegate;
        this.accessorMethod = accessorMethod;
        this.deniedHandler = deniedHandler;
        this.evaluator = evaluator;
    }

    @Override
    public void serializeAsProperty(Object bean, JsonGenerator gen, SerializationContext prov) throws Exception {
        if (isAuthorized(bean)) {
            delegate.serializeAsProperty(bean, gen, prov);
            return;
        }

        if (shouldMask()) {
            gen.writeName(delegate.getName());
            gen.writeString(MaskDeniedHandler.MASK_VALUE);
        }
    }

    @Override
    protected BeanPropertyWriter _new(PropertyName newName) {
        // BeanPropertyWriter._new(PropertyName) is protected, so it cannot be called on an
        // arbitrary `delegate` reference from here — only the public rename(NameTransformer)
        // can. Since rename() dispatches to _new() polymorphically, a transformer that always
        // returns newName's simple name reproduces the same rename on the delegate.
        //
        // rename() compares its transformed name against BeanPropertyWriter._name — always a
        // plain (namespace-free) serialized name, confirmed from BeanPropertyWriter/SerializedString
        // sources — against which PropertyName.getSimpleName() is the correct, non-divergent
        // comparison; getSimpleName() strips any namespace that toString() would include.
        //
        // If the delegate's current name already equals newName, rename() is a no-op and returns
        // the delegate unchanged (BeanPropertyWriter.rename(), line ~346). Short-circuit that case
        // explicitly rather than relying on it implicitly: it avoids allocating a redundant
        // SecuredBeanPropertyWriter and keeps the "did we actually rename" branch visible in code.
        if (newName.getSimpleName().equals(delegate.getName())) {
            return this;
        }

        String targetName = newName.getSimpleName();
        NameTransformer toNewName = new NameTransformer() {
            @Override
            public String transform(String name) {
                return targetName;
            }

            @Override
            public String reverse(String transformed) {
                // Unreachable on this path: reverse() is only used by prefix/suffix transformers
                // to un-mangle a name, and neither BeanPropertyWriter.rename() nor
                // UnwrappingBeanPropertyWriter.rename() calls it on the transformer passed to
                // _new(). This body exists only to satisfy the abstract method.
                return transformed;
            }
        };
        return new SecuredBeanPropertyWriter(delegate.rename(toNewName), accessorMethod, deniedHandler, evaluator);
    }

    @Override
    public BeanPropertyWriter unwrappingWriter(NameTransformer transformer) {
        return new SecuredBeanPropertyWriter(delegate.unwrappingWriter(transformer), accessorMethod, deniedHandler, evaluator);
    }

    private boolean isAuthorized(Object bean) {
        AuthorizationEvaluator resolved = evaluator.get();
        return resolved != null && resolved.canReadField(accessorMethod, bean);
    }

    private boolean shouldMask() {
        return deniedHandler != null
                && MaskDeniedHandler.class.isAssignableFrom(deniedHandler.handlerClass());
    }
}
