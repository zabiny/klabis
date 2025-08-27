# Klabis Forms

1. create DTO object holding form data. Add form validations using JSR-303 annotations.
2. Create class which will prepare form data and process submitted form - implement `FormHandler` interface and annotate
   it with `@FormUseCase`

Created form will get 3 API endpoints:

- `GET /forms/{handlerBeanName}` with `application/json` media type - returns data to be displayed in UI form (= returns
  data returned from `FormHandler.getFormData`)
- `GET /forms/{handlerBeanName}` with `application/yup+json` media type - returns YUP json schema to validate form in
  UI. Schema is generated from Form DTO and it's JSR-303 annotations
- `PUT /forms/{handlerBeanName}` - processes submitted form data (= calls `FormHandler.submitData` method)
