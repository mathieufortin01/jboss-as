/*
 * JBoss, Home of Professional Open Source
 * Copyright 2010, Red Hat Inc., and individual contributors as indicated
 * by the @authors tag. See the copyright.txt in the distribution for a
 * full listing of individual contributors.
 *
 * This is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
 */
package org.jboss.as.ejb3.deployment.processors.merging;

import org.jboss.as.ee.component.EEApplicationClasses;
import org.jboss.as.ee.metadata.MethodAnnotationAggregator;
import org.jboss.as.ee.metadata.RuntimeAnnotationInformation;
import org.jboss.as.ejb3.component.stateful.StatefulComponentDescription;
import org.jboss.as.server.deployment.DeploymentUnit;
import org.jboss.as.server.deployment.DeploymentUnitProcessingException;
import org.jboss.as.server.deployment.reflect.DeploymentReflectionIndex;
import org.jboss.metadata.ejb.spec.SessionBean31MetaData;
import org.jboss.metadata.ejb.spec.SessionBeanMetaData;

import javax.ejb.AfterBegin;
import javax.ejb.AfterCompletion;
import javax.ejb.BeforeCompletion;
import javax.ejb.SessionSynchronization;
import java.lang.reflect.Method;
import java.util.List;
import java.util.Map;

/**
 * Merging processor that handles session synchronization callback methods
 *
 * @author Stuart Douglas
 */
public class SessionSynchronizationMergingProcessor extends AbstractMergingProcessor<StatefulComponentDescription> {

    public SessionSynchronizationMergingProcessor() {
        super(StatefulComponentDescription.class);
    }

    @Override
    protected void handleAnnotations(final DeploymentUnit deploymentUnit, final EEApplicationClasses applicationClasses, final DeploymentReflectionIndex deploymentReflectionIndex, final Class<?> componentClass, final StatefulComponentDescription description) throws DeploymentUnitProcessingException {

        if (SessionSynchronization.class.isAssignableFrom(componentClass)) {
            //handled in handleDeploymentDescriptor
            return;
        }

        RuntimeAnnotationInformation<Boolean> afterBegin = MethodAnnotationAggregator.runtimeAnnotationInformation(componentClass, applicationClasses, deploymentReflectionIndex, AfterBegin.class);
        if (afterBegin.getMethodAnnotations().size() > 1) {
            throw new RuntimeException("Only one @AfterBegin method is allowed on bean " + description.getEJBClassName());
        } else if (!afterBegin.getMethodAnnotations().isEmpty()) {
            Map.Entry<Method, List<Boolean>> entry = afterBegin.getMethodAnnotations().entrySet().iterator().next();
            description.setAfterBegin(entry.getKey().getDeclaringClass().getName(), entry.getKey().getName());
        }

        RuntimeAnnotationInformation<Boolean> afterComp = MethodAnnotationAggregator.runtimeAnnotationInformation(componentClass, applicationClasses, deploymentReflectionIndex, AfterCompletion.class);
        if (afterComp.getMethodAnnotations().size() > 1) {
            throw new RuntimeException("Only one @AfterCompletion method is allowed on bean " + description.getEJBClassName());
        } else if (!afterComp.getMethodAnnotations().isEmpty()) {
            Map.Entry<Method, List<Boolean>> entry = afterComp.getMethodAnnotations().entrySet().iterator().next();
            description.setAfterCompletion(entry.getKey().getDeclaringClass().getName(), entry.getKey().getName());
        }

        RuntimeAnnotationInformation<Boolean> beforeComp = MethodAnnotationAggregator.runtimeAnnotationInformation(componentClass, applicationClasses, deploymentReflectionIndex, BeforeCompletion.class);
        if (beforeComp.getMethodAnnotations().size() > 1) {
            throw new RuntimeException("Only one @BeforeCompletion method is allowed on bean " + description.getEJBClassName());
        } else if (!beforeComp.getMethodAnnotations().isEmpty()) {
            Map.Entry<Method, List<Boolean>> entry = beforeComp.getMethodAnnotations().entrySet().iterator().next();
            description.setBeforeCompletion(entry.getKey().getDeclaringClass().getName(), entry.getKey().getName());
        }

    }

    @Override
    protected void handleDeploymentDescriptor(final DeploymentUnit deploymentUnit, final DeploymentReflectionIndex deploymentReflectionIndex, final Class<?> componentClass, final StatefulComponentDescription description) throws DeploymentUnitProcessingException {
        //if we implement SessionSynchronization we can ignore any DD information
        if (SessionSynchronization.class.isAssignableFrom(componentClass)) {
            description.setAfterBegin(null, "afterBegin");
            description.setAfterCompletion(null, "afterCompletion");
            description.setBeforeCompletion(null, "beforeCompletion");
            return;
        }

        SessionBeanMetaData data = description.getDescriptorData();
        if (data instanceof SessionBean31MetaData) {
            SessionBean31MetaData metaData = (SessionBean31MetaData) data;
            if (metaData.getAfterBeginMethod() != null)
                description.setAfterBegin(null, metaData.getAfterBeginMethod().getMethodName());
            if (metaData.getAfterCompletionMethod() != null)
                description.setAfterCompletion(null, metaData.getAfterCompletionMethod().getMethodName());
            if (metaData.getBeforeCompletionMethod() != null)
                description.setBeforeCompletion(null, metaData.getBeforeCompletionMethod().getMethodName());
        }
    }
}
