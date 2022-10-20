package at.jku.cps.travart.plugin.ppr.dsl.transformation;

import at.jku.cps.travart.core.common.IConfigurable;
import at.jku.cps.travart.core.common.Prop4JUtils;
import at.jku.cps.travart.core.common.TraVarTUtils;
import at.jku.cps.travart.core.common.UVLUtils;
import at.jku.cps.travart.core.exception.NotSupportedVariabilityTypeException;
import at.jku.cps.travart.core.sampling.FeatureModelSampler;
import at.jku.cps.travart.plugin.ppr.dsl.common.PprDslUtils;
import at.jku.cps.travart.plugin.ppr.dsl.exception.NotSupportedConstraintType;
import at.jku.cps.travart.plugin.ppr.dsl.parser.ConstraintDefinitionParser;
import at.sqi.ppr.dsl.reader.constants.DslConstants;
import at.sqi.ppr.model.AssemblySequence;
import at.sqi.ppr.model.NamedObject;
import at.sqi.ppr.model.constraint.Constraint;
import at.sqi.ppr.model.product.Product;
import de.ovgu.featureide.fm.core.base.FeatureUtils;
import de.ovgu.featureide.fm.core.base.IFeature;
import de.ovgu.featureide.fm.core.functional.Functional;
import de.vill.model.Attribute;
import de.vill.model.Feature;
import de.vill.model.FeatureModel;
import org.prop4j.Node;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import static at.jku.cps.travart.plugin.ppr.dsl.transformation.DefaultPprDslTransformationProperties.ATTRIBUTE_DEFAULT_VALUE_KEY_PRAEFIX;
import static at.jku.cps.travart.plugin.ppr.dsl.transformation.DefaultPprDslTransformationProperties.ATTRIBUTE_DESCRIPTION_KEY_PRAEFIX;
import static at.jku.cps.travart.plugin.ppr.dsl.transformation.DefaultPprDslTransformationProperties.ATTRIBUTE_ID_KEY_PRAEFIX;
import static at.jku.cps.travart.plugin.ppr.dsl.transformation.DefaultPprDslTransformationProperties.ATTRIBUTE_TYPE_KEY_PRAEFIX;
import static at.jku.cps.travart.plugin.ppr.dsl.transformation.DefaultPprDslTransformationProperties.ATTRIBUTE_UNIT_KEY_PRAEFIX;
import static at.jku.cps.travart.plugin.ppr.dsl.transformation.DefaultPprDslTransformationProperties.ATTRIBUTE_VALUE_KEY_PRAEFIX;
import static at.jku.cps.travart.plugin.ppr.dsl.transformation.DefaultPprDslTransformationProperties.CHILDREN_PRODUCTS_LIST_NAME_NR_;
import static at.jku.cps.travart.plugin.ppr.dsl.transformation.DefaultPprDslTransformationProperties.CHILDREN_PRODUCTS_LIST_SIZE;
import static at.jku.cps.travart.plugin.ppr.dsl.transformation.DefaultPprDslTransformationProperties.IMPLEMENTED_PRODUCTS_LIST_NAME_NR_;
import static at.jku.cps.travart.plugin.ppr.dsl.transformation.DefaultPprDslTransformationProperties.IMPLEMENTED_PRODUCTS_LIST_SIZE;
import static at.jku.cps.travart.plugin.ppr.dsl.transformation.DefaultPprDslTransformationProperties.NAME_ATTRIBUTE_KEY;
import static at.jku.cps.travart.plugin.ppr.dsl.transformation.DefaultPprDslTransformationProperties.PARTIAL_PRODUCT_ATTRIBUTE;
import static at.jku.cps.travart.plugin.ppr.dsl.transformation.DefaultPprDslTransformationProperties.PARTIAL_PRODUCT_DEFAULT_VALUE;
import static at.jku.cps.travart.plugin.ppr.dsl.transformation.DefaultPprDslTransformationProperties.PARTIAL_PRODUCT_DESCRIPTION;
import static at.jku.cps.travart.plugin.ppr.dsl.transformation.DefaultPprDslTransformationProperties.PARTIAL_PRODUCT_TYPE;

public class FeatureModelToPprDslTransformer {
    private AssemblySequence asq;
    private Set<Map<IConfigurable, Boolean>> samples;

//    public AssemblySequence transform(final FeatureModel model, final Set<Map<IConfigurable, Boolean>> samples)
//            throws NotSupportedVariabilityTypeException {
//        this.samples = samples;
//        return this.transform(model);
//    }

    public AssemblySequence transform(final FeatureModel model)
            throws NotSupportedVariabilityTypeException {
        try {
            this.asq = new AssemblySequence();
            this.convertFeature(model.getRootFeature());
            this.restoreAttributes(model.getRootFeature());
            this.restoreAttributesFromFeatureTree(model.getRootFeature());
            this.convertConstraints(model.getConstraints());
            this.deriveProductsFromSamples(model);
            return this.asq;
        } catch (final NotSupportedConstraintType e) {
            throw new NotSupportedVariabilityTypeException(e);
        }
    }

    private void convertFeature(final Feature feature) throws NotSupportedVariabilityTypeException {
        final Product product = new Product();
        product.setId(feature.getFeatureName());
        product.setName(this.restoreNameFromProperties(feature, product));
        product.setAbstract(FeatureUtils.isAbstract(feature));
        this.restoreAttributesFromProperties(feature, product);
        this.addPartialProductAttribute(product);
        PprDslUtils.addProduct(this.asq, product);

        for (final Feature child : FeatureUtils.getChildren(feature)) {
            this.convertFeature(child);
        }
    }

    private void addPartialProductAttribute(final Product product) {
        if (!PprDslUtils.isPartialProduct(product)) {
            NamedObject attribute = this.asq.getDefinedAttributes().get(PARTIAL_PRODUCT_ATTRIBUTE);
            if (attribute == null) {
                attribute = this.asq.getProductAttributes().get(PARTIAL_PRODUCT_ATTRIBUTE);
            }
            if (attribute == null) {
                attribute = new NamedObject();
                attribute.setName(PARTIAL_PRODUCT_ATTRIBUTE);
                attribute.setType(PARTIAL_PRODUCT_TYPE);
                attribute.setDescription(PARTIAL_PRODUCT_DESCRIPTION);
                attribute.setDefaultValue(PARTIAL_PRODUCT_DEFAULT_VALUE);
                this.asq.getDefinedAttributes().put(PARTIAL_PRODUCT_ATTRIBUTE, attribute);
                this.asq.getProductAttributes().put(PARTIAL_PRODUCT_ATTRIBUTE, attribute);
            }
            attribute.setValue("true");
            product.getAttributes().put(PARTIAL_PRODUCT_ATTRIBUTE, attribute);
        }
    }

    private void restoreAttributes(final Feature feature) {
        final Product product = PprDslUtils.getProduct(this.asq, feature.getFeatureName());
        assert product != null;
        this.restoreChildrenListOfProducts(feature, product);
        this.restoreImplementsListOfProducts(feature, product);
        for (final Feature child : feature.getChildren()) {
            this.restoreAttributes(child);
        }
    }

    private String restoreNameFromProperties(final Feature feature, final Product product) {
        final Attribute nameAttribute = feature.getAttributes().get(NAME_ATTRIBUTE_KEY);
        if (nameAttribute == null) {
            return product.getId();
        }

        return nameAttribute.getValue().toString();
    }

    private void restoreAttributesFromProperties(final Feature feature, final Product product) {
        final List<String> attributeNames = feature.getAttributes().keySet()
                .stream()
                .filter(entry -> entry.startsWith(ATTRIBUTE_ID_KEY_PRAEFIX))
                .map(entry -> entry.substring(ATTRIBUTE_ID_KEY_PRAEFIX.length()))
                .collect(Collectors.toList());

        for (final String attributeName : attributeNames) {
            final NamedObject attribute = new NamedObject();
            attribute.setName(attributeName);

            attribute.setEntityType(DslConstants.ATTRIBUTE_ENTITY);

            final Object descriptionObj = UVLUtils.getAttributeValue(feature, ATTRIBUTE_DESCRIPTION_KEY_PRAEFIX + attributeName);
            if (descriptionObj != null) {
                attribute.setDescription(descriptionObj.toString());
            }

            final Object unitObj = UVLUtils.getAttributeValue(feature, ATTRIBUTE_UNIT_KEY_PRAEFIX + attributeName);
            if (unitObj != null) {
                attribute.setDescription(descriptionObj.toString());
            }

            final Object typeObj = UVLUtils.getAttributeValue(feature, ATTRIBUTE_TYPE_KEY_PRAEFIX + attributeName);
            if (typeObj != null) {
                attribute.setType(typeObj.toString());

                switch (typeObj.toString().toLowerCase()) {
                    case "number":
                        final Double defaultValue = Double.parseDouble(
                                UVLUtils.getAttributeValue(feature, ATTRIBUTE_DEFAULT_VALUE_KEY_PRAEFIX + attributeName).toString()
                        );
                        attribute.setDefaultValue(defaultValue);
                        final Object valueObj = UVLUtils.getAttributeValue(feature, ATTRIBUTE_VALUE_KEY_PRAEFIX + attribute.getName());
                        if (valueObj != null) {
                            final Double value = Double.parseDouble(valueObj.toString());
                            attribute.setValue(value);
                        }
                        break;
                    case "string":
                        attribute.setDefaultValue(UVLUtils.getAttributeValue(feature, ATTRIBUTE_DEFAULT_VALUE_KEY_PRAEFIX + attributeName).toString());
                        attribute.setValue(UVLUtils.getAttributeValue(feature, ATTRIBUTE_VALUE_KEY_PRAEFIX + attribute.getName()));
                        break;
                }
            }
            product.getAttributes().put(attributeName, attribute);
            this.asq.getDefinedAttributes().put(attributeName, attribute);
            this.asq.getProductAttributes().put(attributeName, attribute);
        }
    }

    private void restoreAttributesFromFeatureTree(final Feature feature) {
        // if parent feature is a product, it is an implements relation
        final IFeature parentFeature = FeatureUtils.getParent(feature);
        if (parentFeature != null && FeatureUtils.isAbstract(parentFeature) && !TraVarTUtils.isVirtualRootFeature(parentFeature)) {
            final Product parentProduct = PprDslUtils.getProduct(this.asq, FeatureUtils.getName(parentFeature));
            final Product childProduct = PprDslUtils.getProduct(this.asq, FeatureUtils.getName(feature));
            if (!childProduct.getImplementedProducts().contains(parentProduct)) {
                childProduct.getImplementedProducts().add(parentProduct);
            }
        }
        // if it is an alternative group the excludes constraints have to be derived
        if (FeatureUtils.isAlternative(feature)) {
            for (final IFeature childFeature : FeatureUtils.getChildren(feature)) {
                final Set<IFeature> remChildren = Functional.toSet(FeatureUtils.getChildren(feature));
                remChildren.remove(childFeature);
                final Product childProduct = PprDslUtils.getProduct(this.asq, FeatureUtils.getName(childFeature));
                for (final IFeature other : remChildren) {
                    final Product otherProduct = PprDslUtils.getProduct(this.asq, FeatureUtils.getName(other));
                    childProduct.getExcludes().add(otherProduct);
                }
            }
        }

        for (final Feature child : feature.getChildren()) {
            this.restoreAttributesFromFeatureTree(child);
        }
    }

    private void restoreImplementsListOfProducts(final Feature feature, final Product product) {
        final Object sizeObj = UVLUtils.getAttributeValue(feature, IMPLEMENTED_PRODUCTS_LIST_SIZE);
        if (sizeObj != null) {
            final int size = Integer.parseInt(sizeObj.toString());
            for (int i = 0; i < size; i++) {
                final String productName = UVLUtils.getAttributeValue(feature, IMPLEMENTED_PRODUCTS_LIST_NAME_NR_ + i).toString();
                final Product implementedProduct = PprDslUtils.getProduct(this.asq, productName);
                assert implementedProduct != null;
                product.getImplementedProducts().add(implementedProduct);
            }

        }
    }

    private void restoreChildrenListOfProducts(final Feature feature, final Product product) {
        final Object sizeObj = UVLUtils.getAttributeValue(feature, CHILDREN_PRODUCTS_LIST_SIZE);
        if (sizeObj != null) {
            final int size = Integer.parseInt(sizeObj.toString());
            for (int i = 0; i < size; i++) {
                final String productName =
                        UVLUtils.getAttributeValue(feature, CHILDREN_PRODUCTS_LIST_NAME_NR_ + i).toString();
                final Product childrenProduct = PprDslUtils.getProduct(this.asq, productName);
                assert childrenProduct != null;
                product.getChildren().add(childrenProduct);
            }

        }
    }

    private void convertConstraints(final List<de.vill.model.constraint.Constraint> constraints) throws NotSupportedConstraintType {
        long constrNumber = 0;
        for (final de.vill.model.constraint.Constraint constraint : constraints) {
            final Node node = constraint.getNode().toCNF();
            if (Prop4JUtils.isRequires(node) || Prop4JUtils.isExcludes(node)) {
                final Product left = PprDslUtils.getProduct(this.asq,
                        Prop4JUtils.getLiteralName(Prop4JUtils.getLeftLiteral(node)));
                final Product right = PprDslUtils.getProduct(this.asq,
                        Prop4JUtils.getLiteralName(Prop4JUtils.getRightLiteral(node)));
                if (left != null && right != null && !left.getRequires().contains(right)) {
                    if (Prop4JUtils.isRequires(node)) {
                        left.getRequires().add(right);
                    } else {
                        left.getExcludes().add(right);
                    }
                }
            } else {
                final List<Product> products = constraint.getContainedFeatures().stream()
                        .map(f -> PprDslUtils.getProduct(this.asq, FeatureUtils.getName(f)))
                        .collect(Collectors.toList());
                final String defintion = this.toConstraintDefintion(products, constraint.getNode());
                final Constraint pprConstraint = new Constraint(String.format("Constraint%s", ++constrNumber), defintion);
                this.asq.getGlobalConstraints().add(pprConstraint);
            }
        }
    }

    private String toConstraintDefintion(final List<Product> products, final Node node)
            throws NotSupportedConstraintType {
        final StringBuffer buffer = new StringBuffer();
        for (final Product product : products) {
            buffer.append(product.getId());
            buffer.append(ConstraintDefinitionParser.DELIMITER);
        }
        buffer.deleteCharAt(buffer.lastIndexOf(ConstraintDefinitionParser.DELIMITER));
        buffer.append(" ");
        buffer.append(ConstraintDefinitionParser.DEFINITION_ARROW);
        buffer.append(" ");
        this.toNodeString(buffer, node);
        return buffer.toString();
    }

    private void toNodeString(final StringBuffer buffer, final Node node) throws NotSupportedConstraintType {
        if (node.getMaxDepth() == 1) {
            buffer.append(node.toString());
        } else if (Prop4JUtils.isImplies(node) || Prop4JUtils.isAnd(node) || Prop4JUtils.isOr(node)) {
            this.toNodeString(buffer, Prop4JUtils.getLeftNode(node));
            if (Prop4JUtils.isImplies(node)) {
                buffer.append(" ");
                buffer.append(ConstraintDefinitionParser.IMPLIES);
                buffer.append(" ");
            } else if (Prop4JUtils.isAnd(node)) {
                buffer.append(" ");
                buffer.append(ConstraintDefinitionParser.AND);
                buffer.append(" ");
            } else if (Prop4JUtils.isOr(node)) {
                buffer.append(" ");
                buffer.append(ConstraintDefinitionParser.OR);
                buffer.append(" ");
            }
            this.toNodeString(buffer, Prop4JUtils.getRightNode(node));
        } else if (Prop4JUtils.isNot(node)) {
            buffer.append(" ");
            buffer.append(ConstraintDefinitionParser.NOT);
            buffer.append(" ");
            this.toNodeString(buffer, Prop4JUtils.getLeftNode(node));
        } else {
            throw new NotSupportedConstraintType(node.getClass().toString());
        }
    }

    private void deriveProductsFromSamples(final Feature fm) throws NotSupportedVariabilityTypeException {
        final FeatureModelSampler sampler = new FeatureModelSampler();
        if (this.samples == null) {
            this.samples = sampler.sampleValidConfigurations(fm);
        }
        if (!this.samples.isEmpty()) {
            // create abstract base product for samples
            final String baseProductNameId = FeatureUtils.getName(FeatureUtils.getRoot(fm)).concat("_products");
            final Product baseProduct = new Product(baseProductNameId, baseProductNameId);
            baseProduct.setAbstract(true);
            // union set of keys is the super type requires field
            final Set<String> commonNames = TraVarTUtils.getCommonConfigurationNameSet(this.samples);
            baseProduct.getRequires().addAll(PprDslUtils.getProducts(this.asq, commonNames));
            PprDslUtils.addProduct(this.asq, baseProduct);
            // Create the set of names
            // for each configuration create a sub product ("implements") and set requires
            // with remaining product names
            // collect all created products for later
            final Set<Product> implProducts = new HashSet<>();
            final Set<Product> excludeProducts = new HashSet<>();
            final Set<Set<String>> fmConfigurations = TraVarTUtils.createConfigurationNameSet(this.samples);
            int productNumber = 1;
            for (final Set<String> config : fmConfigurations) {
                config.removeAll(commonNames);
                final String productName = baseProductNameId + String.format("_%s", productNumber++);
                final Product product = new Product(productName, productName);
                product.getImplementedProducts().add(baseProduct);
                product.setAbstract(false);
                product.getRequires().addAll(PprDslUtils.getProducts(this.asq, config));
                PprDslUtils.addProduct(this.asq, product);
                implProducts.add(product);
                excludeProducts.add(product);
            }
            // the created products exclude each other, so we have to iterate again and
            // add the excludes constraints
            for (final Product implP : implProducts) {
                for (final Product excProduct : excludeProducts) {
                    if (!implP.equals(excProduct)) {
                        excProduct.getExcludes().add(implP);
                    }
                }
            }
        }
    }

}